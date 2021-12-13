/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.router.script;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import javax.script.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.cluster.Constants.*;

/**
 * ScriptRouter
 */
public class ScriptRouter extends AbstractRouter {
    public static final String NAME = "SCRIPT_ROUTER";
    private static final int SCRIPT_ROUTER_DEFAULT_PRIORITY = 0;
    private static final Logger logger = LoggerFactory.getLogger(ScriptRouter.class);

    //缓存脚本引擎，所有的ScriptRouter共用
    private static final Map<String, ScriptEngine> ENGINES = new ConcurrentHashMap<>();

    //当前Router使用的java脚本引擎
    private final ScriptEngine engine;

    //脚本内容
    private final String rule;

    //编译之后的脚本
    private CompiledScript function;

    public ScriptRouter(URL url) {
        this.url = url;
        this.priority = url.getParameter(PRIORITY_KEY, SCRIPT_ROUTER_DEFAULT_PRIORITY);

        engine = getEngine(url);
        rule = getRule(url);
        try {
            Compilable compilable = (Compilable) engine;
            function = compilable.compile(rule);
        } catch (ScriptException e) {
            logger.error("route error, rule has been ignored. rule: " + rule +
                    ", url: " + RpcContext.getContext().getUrl(), e);
        }


    }

    /**
     * get rule from url parameters.
     */
    private String getRule(URL url) {
        String vRule = url.getParameterAndDecoded(RULE_KEY);
        if (StringUtils.isEmpty(vRule)) {
            throw new IllegalStateException("route rule can not be empty.");
        }
        return vRule;
    }

    /**
     * create ScriptEngine instance by type from url parameters, then cache it
     */
    private ScriptEngine getEngine(URL url) {
        String type = url.getParameter(TYPE_KEY, DEFAULT_SCRIPT_TYPE_KEY);

        return ENGINES.computeIfAbsent(type, t -> {
            ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(type);
            if (scriptEngine == null) {
                throw new IllegalStateException("unsupported route engine type: " + type);
            }
            return scriptEngine;
        });
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        try {
            Bindings bindings = createBindings(invokers, invocation);
            if (function == null) {
                return invokers;
            }
            //执行并且转换结果
            return getRoutedInvokers(function.eval(bindings));
        } catch (ScriptException e) {
            //如果脚本错误，则不使用该router
            logger.error("route error, rule has been ignored. rule: " + rule + ", method:" +
                    invocation.getMethodName() + ", url: " + RpcContext.getContext().getUrl(), e);
            return invokers;
        }
    }

    /**
     * get routed invokers from result of script rule evaluation
     */
    @SuppressWarnings("unchecked")
    protected <T> List<Invoker<T>> getRoutedInvokers(Object obj) {
        if (obj instanceof Invoker[]) {
            return Arrays.asList((Invoker<T>[]) obj);
        } else if (obj instanceof Object[]) {
            return Arrays.stream((Object[]) obj).map(item -> (Invoker<T>) item).collect(Collectors.toList());
        } else {
            return (List<Invoker<T>>) obj;
        }
    }

    /**
     * create bindings for script engine
     */
    private <T> Bindings createBindings(List<Invoker<T>> invokers, Invocation invocation) {
        //脚本的入参
        //e.g:
        // function route(invokers, invocation, context){
        //  var result = new java.util.ArrayList(invokers.size());
        //	var targetHost = new java.util.ArrayList();
        //	targetHost.add("10.134.108.2");
        //	for (var i = 0; i < invokers.length; i) {//遍历Invoker集合
        //        // 判断Invoker的host是否符合条件
        //		if(targetHost.contains(invokers[i].getUrl().getHost())){
        //			result.add(invokers[i]);
        //		}
        //	}
        //	return result;
        //}
        //route(invokers, invocation, context) //立即执行route()函数
        Bindings bindings = engine.createBindings();
        // create a new List of invokers
        bindings.put("invokers", new ArrayList<>(invokers));
        bindings.put("invocation", invocation);
        bindings.put("context", RpcContext.getContext());
        return bindings;
    }

    @Override
    public boolean isRuntime() {
        return this.url.getParameter(RUNTIME_KEY, false);
    }

    @Override
    public boolean isForce() {
        return url.getParameter(FORCE_KEY, false);
    }

}
