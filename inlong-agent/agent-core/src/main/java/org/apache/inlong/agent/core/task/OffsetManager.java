/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.core.task;

import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.InstanceProfile;
import org.apache.inlong.agent.conf.OffsetProfile;
import org.apache.inlong.agent.conf.TaskProfile;
import org.apache.inlong.agent.constant.CycleUnitType;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.store.InstanceStore;
import org.apache.inlong.agent.store.OffsetStore;
import org.apache.inlong.agent.store.Store;
import org.apache.inlong.agent.store.TaskStore;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.DateTransUtils;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.apache.inlong.common.enums.InstanceStateEnum;
import org.apache.inlong.common.enums.TaskStateEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.inlong.agent.constant.TaskConstants.TASK_AUDIT_VERSION;

/**
 * used to save instance offset to offset store
 * where key is task id + read file name and value is instance offset
 */
public class OffsetManager extends AbstractDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(OffsetManager.class);
    public static final int CORE_THREAD_SLEEP_TIME = 60 * 1000;
    public static final int CLEAN_INSTANCE_ONCE_LIMIT = 100;
    public static final String DB_INSTANCE_EXPIRE_CYCLE_COUNT = "3";
    private static volatile OffsetManager offsetManager = null;
    private final OffsetStore offsetStore;
    private final InstanceStore instanceStore;
    private final TaskStore taskStore;

    private OffsetManager(
            Store taskBasicStore, Store instanceBasicStore,
            Store offsetBasicStore) {
        taskStore = new TaskStore(taskBasicStore);
        instanceStore = new InstanceStore(instanceBasicStore);
        offsetStore = new OffsetStore(offsetBasicStore);
    }

    /**
     * thread for core thread.
     *
     * @return runnable profile.
     */
    private Runnable coreThread() {
        return () -> {
            Thread.currentThread().setName("offset-manager-core");
            while (isRunnable()) {
                try {
                    AgentUtils.silenceSleepInMs(CORE_THREAD_SLEEP_TIME);
                    cleanDbInstance();
                    cleanDbOffset();
                } catch (Throwable ex) {
                    LOGGER.error("offset-manager-core: ", ex);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
                }
            }
        };
    }

    /**
     * task position manager singleton, can only generated by agent manager
     */
    public static void init(
            Store taskBasicStore, Store instanceBasicStore,
            Store offsetBasicStore) {
        if (offsetManager == null) {
            synchronized (OffsetManager.class) {
                if (offsetManager == null) {
                    offsetManager = new OffsetManager(taskBasicStore, instanceBasicStore,
                            offsetBasicStore);
                }
            }
        }
    }

    /**
     * get taskPositionManager singleton
     */
    public static OffsetManager getInstance() {
        if (offsetManager == null) {
            throw new RuntimeException("task position manager has not been initialized by agentManager");
        }
        return offsetManager;
    }

    public void setOffset(OffsetProfile profile) {
        offsetStore.setOffset(profile);
    }

    public void deleteOffset(String taskId, String instanceId) {
        offsetStore.deleteOffset(taskId, instanceId);
    }

    public OffsetProfile getOffset(String taskId, String instanceId) {
        return offsetStore.getOffset(taskId, instanceId);
    }

    private void cleanDbOffset() {
        List<OffsetProfile> offsets = offsetStore.listAllOffsets();
        offsets.forEach(offset -> {
            String taskId = offset.getTaskId();
            String instanceId = offset.getInstanceId();
            InstanceProfile instanceProfile = instanceStore.getInstance(taskId, instanceId);
            if (instanceProfile == null) {
                deleteOffset(taskId, instanceId);
                LOGGER.info("instance not found, delete offset taskId {} instanceId {}", taskId,
                        instanceId);
            }
        });
        LOGGER.info("offsetManager running! offsets count {}", offsets.size());
    }

    private void cleanDbInstance() {
        AtomicInteger cleanCount = new AtomicInteger();
        Iterator<InstanceProfile> iterator = instanceStore.listAllInstances().listIterator();
        while (iterator.hasNext()) {
            if (cleanCount.get() > CLEAN_INSTANCE_ONCE_LIMIT) {
                return;
            }
            InstanceProfile instanceFromDb = iterator.next();
            String taskId = instanceFromDb.getTaskId();
            String instanceId = instanceFromDb.getInstanceId();
            TaskProfile taskFromDb = taskStore.getTask(taskId);
            if (taskFromDb != null) {
                if (taskFromDb.getCycleUnit().compareToIgnoreCase(CycleUnitType.REAL_TIME) == 0) {
                    continue;
                }
                if (taskFromDb.isRetry()) {
                    if (taskFromDb.getState() != TaskStateEnum.RETRY_FINISH) {
                        continue;
                    }
                } else {
                    if (instanceFromDb.getState() != InstanceStateEnum.FINISHED) {
                        continue;
                    }
                }
            }
            long expireTime = DateTransUtils.calcOffset(
                    DB_INSTANCE_EXPIRE_CYCLE_COUNT + instanceFromDb.getCycleUnit());
            if (AgentUtils.getCurrentTime() - instanceFromDb.getModifyTime() > expireTime) {
                cleanCount.getAndIncrement();
                LOGGER.info("instance has expired, delete from instance store dataTime {} taskId {} instanceId {}",
                        instanceFromDb.getSourceDataTime(), taskId, instanceId);
                instanceStore.deleteInstance(taskId, instanceId);
                AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_DEL_INSTANCE_DB, instanceFromDb.getInlongGroupId(),
                        instanceFromDb.getInlongStreamId(), instanceFromDb.getSinkDataTime(), 1, 1,
                        Long.parseLong(instanceFromDb.get(TASK_AUDIT_VERSION)));
                iterator.remove();
            }
        }
    }

    public int getRunningInstanceCount() {
        return instanceStore.getRunningInstanceCount();
    }

    @Override
    public void start() throws Exception {
        submitWorker(coreThread());
    }

    @Override
    public void stop() throws Exception {

    }
}
