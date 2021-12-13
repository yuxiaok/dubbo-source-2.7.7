package org.apache.dubbo.demo.provider.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
@Activate(group = CommonConstants.PROVIDER, order = -1)
public class JavaJarProviderFilter implements Filter {

	private static final String JAR_VERSION_NAME_KEY = "dubbo.jar.version";
	private static final Map<String, AtomicLong> versionState = new ConcurrentHashMap();
	private static final Long STATISTICS_INTERVAL = 10L;
	private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE =
			Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("dubbo-jar-version"));

	public JavaJarProviderFilter() {
		SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(() -> {
			for (Map.Entry<String, AtomicLong> entry : versionState.entrySet()) {
				System.out.println(entry.getKey() + ":" + entry.getValue().getAndSet(0));
			}
		}, STATISTICS_INTERVAL, STATISTICS_INTERVAL, TimeUnit.MINUTES);
	}

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		String jarVersion = invocation.getAttachment(JAR_VERSION_NAME_KEY);
		if (StringUtils.isNotBlank(jarVersion)) {
			AtomicLong atomicLong = versionState.computeIfAbsent(jarVersion, k -> new AtomicLong(0L));
			atomicLong.getAndIncrement();
		}
		return invoker.invoke(invocation);
	}
}