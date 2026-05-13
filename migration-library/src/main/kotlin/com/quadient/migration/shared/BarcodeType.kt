package com.quadient.migration.shared

import kotlinx.serialization.Serializable

@Serializable
enum class BarcodeType {
    Code39, QR_CODE
}

@Serializable
enum class QrCodeErrorCorrectionLevel(val inspireValue: Int) {
    L(76), M(77), Q(81), H(72),
}

@Serializable
enum class QrCodeSize(val inspireValue: Int?) {
    Auto(null), Fixed21x21(21), Fixed25x25(25), Fixed29x29(29), Fixed33x33(33), Fixed37x37(37), Fixed41x41(41), Fixed45x45(
        45
    ),
    Fixed49x49(49), Fixed53x53(53), Fixed57x57(57), Fixed61x61(61), Fixed65x65(65), Fixed69x69(69), Fixed73x73(73), Fixed77x77(
        77
    ),
    Fixed81x81(81), Fixed85x85(85), Fixed89x89(89), Fixed93x93(93), Fixed97x97(97), Fixed101x101(101), Fixed105x105(105), Fixed109x109(
        109
    ),
    Fixed113x113(113), Fixed117x117(117), Fixed121x121(121), Fixed125x125(125), Fixed129x129(129), Fixed133x133(133), Fixed137x137(
        137
    ),
    Fixed141x141(141), Fixed145x145(145), Fixed149x149(149), Fixed153x153(153), Fixed157x157(157), Fixed161x161(161), Fixed165x165(
        165
    ),
    Fixed169x169(169), Fixed173x173(173), Fixed177x177(177),
}

@Serializable
enum class EciCode(val inspireValue: Int?) {
    None(null), ISO_IEC_15438_DEFAULT_GLI(0), ISO_IEC_15438_LATIN1(1), ISO_IEC_15438_DEFAULT_ECI(2), ISO_IEC_8859_1(3), ISO_IEC_8859_2(
        4
    ),
    ISO_IEC_8859_3(5), ISO_IEC_8859_4(6), ISO_IEC_8859_5(7), ISO_IEC_8859_6(8), ISO_IEC_8859_7(9), ISO_IEC_8859_8(10), ISO_IEC_8859_9(
        11
    ),
    ISO_IEC_8859_13(15), ISO_IEC_8859_15(17), SHIFT_JIS(20), WINDOWS_1250(21), WINDOWS_1251(22), WINDOWS_1252(23), WINDOWS_1256(
        24
    ),
    ISO_IEC_10646_UCS_2(25), ISO_IEC_10646_UTF_8(26), ISO_IEC_646_1991(27), BIG_5(28), GB_PRC(29), KOREAN_CHARSET(30), ISO_IEC_646_ISO_7BIT_CODED_INVARIANT(
        170
    ),
}
