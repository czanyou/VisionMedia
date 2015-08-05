/***
 * The content of this file or document is CONFIDENTIAL and PROPRIETARY
 * to ChengZhen(anyou@msn.com). It is subject to the terms of a
 * License Agreement between Licensee and ChengZhen(anyou@msn.com).
 * restricting among other things, the use, reproduction, distribution
 * and transfer. Each of the embodiments, including this information and
 * any derivative work shall retain this copyright notice.
 *
 * Copyright (c) 2005-2014 ChengZhen(anyou@msn.com) All Rights Reserved.
 *
 */
package org.vision.media.mp4;

/**
 * MP4 Atom 属性的类型.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public enum Mp4PropertyType {

	/** 整型属性. 包括 1, 2, 3, 4, 8 个字节的整数 */
	PT_INT,

	/** 日期型属性, 4 个字节.  表示 1904 年以来经过的秒数*/
	PT_DATE,

	/** 浮点型, 长 4 个或 2 个字节. */
	PT_FLOAT,

	/** 比特类型属性. 1 到 8 个比特. */
	PT_BITS,

	/** 字节数组类型属性. */
	PT_BYTES,

	/** 字符串类型属性. */
	PT_STRING,

	/** 表格类型属性. */
	PT_TABLE,

	/** 字节数组表格类型属性. */
	PT_SIZE_TABLE,

	/** 描述型属性. */
	PT_DESCRIPTOR,
}
