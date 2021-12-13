package org.apache.dubbo.demo.consumer.filter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * <p>****************************************************************************
 * </p>
 * <p><b>Copyright © 2010-2021 shuncom team All Rights Reserved<b></p>
 * <ul style="margin:15px;">
 * <li>Description : TODO </li>
 * <li>Version : 1.0.0</li>
 * <li>Creation : 2021年11⽉20⽇</li>
 * <li>@author : 8201</li>
 * </ul>
 * <p>****************************************************************************
 * </p>
 */
@Activate(group = CommonConstants.CONSUMER, order = -1)
public class JavaJarConsumerFilter implements Filter {

	private static final String JAR_VERSION_NAME_KEY = "dubbo.jar.version";

	private LoadingCache<Class<?>, String> versionCache = CacheBuilder.newBuilder()
			.maximumSize(1024)
			.build(new CacheLoader<Class<?>, String>() {
				@Override
				public String load(Class<?> aClass) throws Exception {
					return getVersion(aClass);
				}
			});

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		String jarVersion = versionCache.getUnchecked(invoker.getInterface());
		if (!StringUtils.isBlank(jarVersion)) {
			invocation.getAttachments().put(JAR_VERSION_NAME_KEY, jarVersion);
		}
		return invoker.invoke(invocation);
	}


	private String getVersion(Class<?> clazz) {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clazz.getResourceAsStream("/META-INF/MANIFEST.MF")))) {
			String s = null;
			while ((s = bufferedReader.readLine()) != null) {
				int i = s.indexOf("Implementation-Version:");
				if (i > 0) {
					return s.substring(i);
				}
			}
		} catch (Exception e) {

		}
		return "";
	}
}