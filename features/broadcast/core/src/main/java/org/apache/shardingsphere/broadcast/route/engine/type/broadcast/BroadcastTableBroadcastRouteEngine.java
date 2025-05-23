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

package org.apache.shardingsphere.broadcast.route.engine.type.broadcast;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.broadcast.route.engine.type.BroadcastRouteEngine;
import org.apache.shardingsphere.broadcast.rule.BroadcastRule;
import org.apache.shardingsphere.infra.annotation.HighFrequencyInvocation;
import org.apache.shardingsphere.infra.route.context.RouteContext;
import org.apache.shardingsphere.infra.route.context.RouteMapper;
import org.apache.shardingsphere.infra.route.context.RouteUnit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Broadcast route engine for table.
 */
@HighFrequencyInvocation
@RequiredArgsConstructor
public final class BroadcastTableBroadcastRouteEngine implements BroadcastRouteEngine {
    
    private final Collection<String> broadcastTableNames;
    
    @Override
    public RouteContext route(final BroadcastRule rule) {
        RouteContext result = new RouteContext();
        Collection<String> logicTableNames = rule.getBroadcastTableNames(broadcastTableNames);
        RouteContext toBeAddedRouteContext = logicTableNames.isEmpty() ? getRouteContext(rule) : getRouteContext(rule, logicTableNames);
        result.getRouteUnits().addAll(toBeAddedRouteContext.getRouteUnits());
        return result;
    }
    
    private RouteContext getRouteContext(final BroadcastRule rule) {
        RouteContext result = new RouteContext();
        for (String each : rule.getDataSourceNames()) {
            result.getRouteUnits().add(new RouteUnit(new RouteMapper(each, each), Collections.singletonList(new RouteMapper("", ""))));
        }
        return result;
    }
    
    private RouteContext getRouteContext(final BroadcastRule rule, final Collection<String> logicTableNames) {
        RouteContext result = new RouteContext();
        Collection<RouteMapper> tableRouteMappers = getTableRouteMappers(logicTableNames);
        for (String each : rule.getDataSourceNames()) {
            RouteMapper dataSourceMapper = new RouteMapper(each, each);
            result.getRouteUnits().add(new RouteUnit(dataSourceMapper, tableRouteMappers));
        }
        return result;
    }
    
    private Collection<RouteMapper> getTableRouteMappers(final Collection<String> logicTableNames) {
        Collection<RouteMapper> result = new LinkedList<>();
        for (String each : logicTableNames) {
            result.add(new RouteMapper(each, each));
        }
        return result;
    }
}
