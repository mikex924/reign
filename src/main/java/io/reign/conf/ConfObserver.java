/*
 * Copyright 2013 Yen Pai ypai@reign.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.reign.conf;

import io.reign.AbstractObserver;
import io.reign.DataSerializer;

import java.util.List;

/**
 * 
 * @author ypai
 * 
 */
public abstract class ConfObserver<T> extends AbstractObserver {

    private String clusterId = null;
    private String serviceId = null;
    private String nodeId = null;

    public abstract void updated(T updated, T existing);

    void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void nodeDataChanged(byte[] updatedData, byte[] previousData) {
        updated(fromBytes(updatedData), fromBytes(previousData));
    }

    @Override
    public void nodeDeleted(byte[] previousData, List<String> previousChildList) {
        updated(null, fromBytes(previousData));
    }

    @Override
    public void nodeCreated(byte[] data, List<String> childList) {
        T conf = fromBytes(data);
        if (conf != null) {
            updated(conf, null);
        }
    }

    public T fromBytes(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        DataSerializer<T> transcoder = DefaultConfService.DEFAULT_CONF_SERIALIZER;
        return transcoder.deserialize(data);
    }

}
