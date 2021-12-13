package org.apache.dubbo.demo;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

/**
 * <p>****************************************************************************
 * </p>
 * <p><b>Copyright © 2010-2021 shuncom team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : TODO </li>
 * <li>Version : 1.0.0</li>
 * <li>Creation : 2021年12⽉05⽇</li>
 * <li>@author : 8201</li>
 * </ul>
 * <p>****************************************************************************
 * </p>
 */
public class StringMerger implements Merger<String> {

	@Override
	public String merge(String... items) {
		if (ArrayUtils.isEmpty(items)) {
			return "";
		}
		String res = "";
		for (String item : items) {
			res += item + " | ";
		}
		return res;
	}
}