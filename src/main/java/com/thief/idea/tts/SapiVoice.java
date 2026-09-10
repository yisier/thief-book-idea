package com.thief.idea.tts;

import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.COMLateBindingObject;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.IDispatch;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Guid.REFIID;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.OaIdl.DISPID;
import com.sun.jna.platform.win32.OaIdl.DISPIDByReference;
import com.sun.jna.platform.win32.OaIdl.EXCEPINFO;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Windows SAPI 的 SpVoice 自动化对象封装（JNA late binding）。
 * 仅可在已 CoInitialize 的线程上创建/调用。
 **/
public class SapiVoice extends COMLateBindingObject {

    /**
     * Speak 标志：异步（立即返回，后台朗读）
     **/
    private static final int SPF_ASYNC = 1;
    /**
     * Speak 标志：清空朗读队列中未播报的内容
     **/
    private static final int SPF_PURGEBEFORESPEAK = 2;
    /**
     * Speak 标志：按纯文本处理，不解析 XML/SSML
     **/
    private static final int SPF_IS_NOT_XML = 16;

    /**
     * SpeechRunState：等待开始（尚未朗读）
     **/
    public static final int RS_WAITING = 0;
    /**
     * SpeechRunState：已读完所有排队内容
     **/
    public static final int RS_DONE = 1;
    /**
     * SpeechRunState：正在朗读（占用音频队列）
     **/
    public static final int RS_SPEAKING = 2;

    public SapiVoice() {
        super("SAPI.SpVoice", false);
    }

    public SapiVoice(IDispatch dispatch) {
        super(dispatch);
    }

    /**
     * 朗读纯文本（异步），随后用 runningState()/waitUntilDone() 轮询结束
     **/
    public void speakAsync(String text) {
        invokeNoReply("Speak", new VARIANT[]{
                new VARIANT(text),
                new VARIANT(SPF_ASYNC | SPF_IS_NOT_XML)
        });
    }

    /**
     * 清空当前及排队朗读
     **/
    public void purge() {
        invokeNoReply("Speak", new VARIANT[]{
                new VARIANT(""),
                new VARIANT(SPF_ASYNC | SPF_PURGEBEFORESPEAK)
        });
    }

    /**
     * 阻塞至多 msTimeout 毫秒，等待当前朗读结束
     **/
    public void waitUntilDone(int msTimeout) {
        invokeNoReply("WaitUntilDone", new VARIANT[]{new VARIANT(msTimeout)});
    }

    public void pause() {
        invokeNoReply("Pause");
    }

    public void resume() {
        invokeNoReply("Resume");
    }

    /**
     * 读取当前运行状态（SpeechRunState：0 等待 / 1 读完 / 2 朗读中）。
     * 每轮 fresh 取一次 Status（该状态对象是快照），最后用 VariantClear 释放其引用，
     * 避免长时间轮询泄漏；注意包装对象不再单独 release，否则会重复释放。
     **/
    public int runningState() {
        VARIANT.ByReference statusVariant = new VARIANT.ByReference();
        oleMethod(OleAuto.DISPATCH_PROPERTYGET, statusVariant, "Status");
        try {
            IDispatch status = (IDispatch) statusVariant.getValue();
            return new SapiVoice(status).getIntProperty("RunningState");
        } finally {
            OleAuto.INSTANCE.VariantClear(statusVariant);
        }
    }

    /**
     * 语速倍率转 SAPI 的 -10 ~ 10（0 为正常），并固定音量 100
     **/
    public void applyRate(double rate) {
        int sapiRate = (int) Math.round((rate - 1.0) * 10.0);
        if (sapiRate < -10) {
            sapiRate = -10;
        } else if (sapiRate > 10) {
            sapiRate = 10;
        }
        setProperty("Rate", sapiRate);
        setProperty("Volume", 100);
    }

    /**
     * 全部语音显示名（GetDescription）
     **/
    public List<String> listVoiceDescriptions() {
        List<String> result = new ArrayList<>();
        IDispatch tokens = getAutomationProperty("GetVoices");
        SapiVoice collection = new SapiVoice(tokens);
        try {
            int count = collection.getIntProperty("Count");
            for (int i = 0; i < count; i++) {
                IDispatch token = collection.getAutomationProperty("Item", new VARIANT(i));
                SapiVoice tokenVoice = new SapiVoice(token);
                try {
                    result.add(tokenVoice.getStringProperty("GetDescription"));
                } finally {
                    tokenVoice.release();
                }
            }
        } finally {
            collection.release();
        }
        return result;
    }

    /**
     * 按显示名精确/包含匹配选择语音；name 为空时保持系统默认。
     * 返回是否找到并设置成功。
     **/
    public boolean setVoiceByName(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        IDispatch tokens = getAutomationProperty("GetVoices");
        SapiVoice collection = new SapiVoice(tokens);
        List<SapiVoice> tokenWrappers = new ArrayList<>();
        try {
            int count = collection.getIntProperty("Count");
            IDispatch matched = null;
            IDispatch fallback = null;
            for (int i = 0; i < count; i++) {
                IDispatch token = collection.getAutomationProperty("Item", new VARIANT(i));
                SapiVoice tokenVoice = new SapiVoice(token);
                tokenWrappers.add(tokenVoice);
                String desc = tokenVoice.getStringProperty("GetDescription");
                if (name.equals(desc)) {
                    matched = token;
                    break;
                }
                if (fallback == null && desc.toLowerCase().contains(name.toLowerCase())) {
                    fallback = token;
                }
            }
            IDispatch target = matched != null ? matched : fallback;
            if (target == null) {
                return false;
            }
            // 必须先用完 target 再释放包装对象，避免先 Release 导致悬空引用
            putRefProperty("Voice", target);
            return true;
        } finally {
            for (SapiVoice wrapper : tokenWrappers) {
                wrapper.releaseQuietly();
            }
            collection.releaseQuietly();
        }
    }

    /**
     * 以 PROPERTYPUTREF 设置对象属性（SAPI 的 Voice 属性只支持引用赋值；
     * JNA 的 setProperty(String, IDispatch) 用的是 PROPERTYPUT，会报成员不存在）。
     **/
    private void putRefProperty(String name, IDispatch value) {
        IDispatch dispatch = getIDispatch();
        DISPIDByReference dispId = new DISPIDByReference();
        HRESULT hr = dispatch.GetIDsOfNames(new REFIID(Guid.IID_NULL),
                new WString[]{new WString(name)}, 1, LOCALE_SYSTEM_DEFAULT, dispId);
        COMUtils.checkRC(hr);

        OleAuto.DISPPARAMS.ByReference params = new OleAuto.DISPPARAMS.ByReference();
        params.setRgdispidNamedArgs(new DISPID[]{OaIdl.DISPID_PROPERTYPUT});
        params.setArgs(new VARIANT[]{new VARIANT(value)});
        params.write();

        EXCEPINFO.ByReference excepInfo = new EXCEPINFO.ByReference();
        IntByReference argErr = new IntByReference();
        hr = dispatch.Invoke(dispId.getValue(), new REFIID(Guid.IID_NULL), LOCALE_SYSTEM_DEFAULT,
                new WinDef.WORD(OleAuto.DISPATCH_PROPERTYPUTREF), params, null, excepInfo, argErr);
        COMUtils.checkRC(hr, excepInfo, argErr);
    }

    /**
     * 释放 COM 引用，忽略异常（用于临时包装对象）
     **/
    public void releaseQuietly() {
        try {
            release();
        } catch (Throwable ignored) {
        }
    }
}
