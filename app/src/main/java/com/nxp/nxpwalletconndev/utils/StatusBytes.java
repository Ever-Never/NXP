package com.nxp.nxpwalletconndev.utils;

public class StatusBytes {


    /*Status values*/
    public static final int SUCCESS             =   0;
    public static final int FAILED              =   1;
    public static final int NOT_IN_USE          =   2;
    public static final int IN_USE              =   3;

    public static final short SW_NO_ERROR = (short)0x9000;

    /**
     * Generic error codes (from ltsm client)
     * */
    public static final short SW_INVALID_VC_ENTRY                   = (short)0x69E0;
    public static final short SW_OPEN_LOGICAL_CHANNEL_FAILED        = (short)0x69E1;
    public static final short SW_SELECT_LTSM_FAILED                 = (short)0x69E2;
    public static final short SW_REGISTRY_FULL                      = (short)0x69E3;
    public static final short SW_IMPROPER_REGISTRY                  = (short)0x69E4;
    public static final short SW_NO_SET_SP_SD                       = (short)0x69E5;
    public static final short SW_ALREADY_ACTIVATED                  = (short)0x69E6;
    public static final short SW_ALREADY_DEACTIVATED                = (short)0x69E7;
    public static final short SW_CRS_SELECT_FAILED                  = (short)0x69E8;
    public static final short SW_SE_TRANSCEIVE_FAILED               = (short)0x69E9;
    public static final short SW_REGISTRY_IS_EMPTY                  = (short)0x69EA;

    public static final short SW_OTHER_ACTIVEVC_EXIST               = (short)0x6330;
    public static final short SW_PROCESSING_ERROR                   = (short)0x6A88;
    public static final short SW_INCORRECT_PARAMETERS               = (short)0x6A80;

    /**
     * createVc Response Error Codes (from ltsm application)
     * */
    public static final short CREATEVC_PREPERSO_NOT_FOUND                = (short)0x6F01;
    public static final short CREATEVC_SET_SP_SD_NOT_SENT                = (short)0x6F02;
    public static final short CREATEVC_INVALID_SIGNATURE                 = (short)0x6F03;
    public static final short CREATEVC_SET_SP_SD_SENT                    = (short)0x6F04;
    public static final short CREATEVC_REGISTRY_ENTRY_CREATION_FAILED    = (short)0x6F05;

    /**
     * addAndUpdate/deleteVc Response Error Codes (from ltsm application)
     * */
    public static final short ADDANDUPDATE_INVALID_VC_ENTRY                  = (short)0x6F01; //Same for deleteVc
    public static final short ADDANDUPDATE_NO_LTSM_SD_PRESENT                = (short)0x6F02;

    /**
     * readMifareDataVc Response Error Codes
     */
    public static final short AUTHENTICATION_ERROR				    = (short)0x63E9;

    /**
     * getVcList Response Error Codes (from ltsm application)
     * */
    public static final short NO_VC_ENTRY_PRESENT                   = (short)0x6A88;
}
