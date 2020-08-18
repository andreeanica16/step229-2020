// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Date;
import java.sql.Timestamp;
import java.util.List;
import com.google.api.services.dataflow.Dataflow;
import com.google.api.services.dataflow.model.Job;
import com.google.api.services.dataflow.model.JobMetadata;
import com.google.api.services.dataflow.model.SdkVersion;
import com.google.api.services.dataflow.model.Environment;
import com.google.api.services.dataflow.model.WorkerPool;
import com.google.api.services.dataflow.model.JobMetrics;
import com.google.api.services.dataflow.model.MetricUpdate;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.math.BigDecimal;



// Class used to represent a Job in memory, extracting just the 
// information required by the analysis
public abstract class JobModel {
    public static final Integer SSD = 1;
    public static final Integer HDD = 2;
    String projectId;
    String name;
    String id;

    // Type can have following values:
    // - JOB_TYPE_STREAMING
    // - JOB_TYPE_UNKNOWN
    // - JOB_TYPE_BATCH
    String type;
    String sdk = null;
    // sdkSupportStatus ca be found at 
    // https://cloud.google.com/dataflow/docs/reference/rest/v1b3/projects.jobs#sdksupportstatus
    String sdkSupportStatus = null;
    String region;
    int currentWorkers;
    String startTime;

    // Following fields stores the number of seconds
    Long totalVCPUTime = null; // divide by 3600
    Long totalMemoryTime = null; // divide by (3600 * 1024)
    Long totalDiskTime = null;
    Integer diskType = null;
    Double currentVcpuCount = null;
    Long totalStreamingData = null; //  multiply by 1024
    Boolean enableStreamingEngine = false;
    String metricTime = null;

    // Possible states can be found at 
    // https://cloud.google.com/dataflow/docs/reference/rest/v1b3/projects.jobs#jobstate
    String state;
    String stateTime;

    public JobModel(String projectId, Job job, Dataflow dataflowService) throws IOException, IllegalArgumentException {
      this.projectId = projectId;
      name = job.getName();
      id = job.getId();
      type = job.getType();
      
      SdkVersion sdkVersion = job.getJobMetadata().getSdkVersion();
      if (sdkVersion != null) {
          sdk = sdkVersion.getVersion();
          sdkSupportStatus =  sdkVersion.getSdkSupportStatus();
      }

      region = job.getLocation();

      Environment environment = job.getEnvironment();
      if (environment != null) {;

          currentWorkers = 0;
          List<WorkerPool> workerPoolList = environment.getWorkerPools();
          if (workerPoolList != null) {
            for (WorkerPool workerPool : workerPoolList) {
              currentWorkers += workerPool.getNumWorkers();
            }
          }
      }

      startTime = job.getStartTime();

      getMetrics(dataflowService);
    }

    private void getMetrics(Dataflow dataflowService) throws IOException, IllegalArgumentException {
        Dataflow.Projects.Locations.Jobs.GetMetrics request2 = dataflowService.projects()
        .locations()
        .jobs()
        .getMetrics(projectId, region, id);
        JobMetrics jobMetric = request2.execute();

        metricTime = jobMetric.getMetricTime();

        if (jobMetric != null) {
          // The job has failed, so it has no metrics
          if (jobMetric.getMetrics() == null) {
              return;
          } 
          for (MetricUpdate metric : jobMetric.getMetrics()) {
            String metricName = metric.getName().getName();
            if (metricName.compareTo("TotalVcpuTime") == 0) {
              totalVCPUTime = ((BigDecimal) metric.getScalar()).longValue();
            } else if (metricName.compareTo("TotalMemoryUsage") == 0) {
              totalMemoryTime = ((BigDecimal) metric.getScalar()).longValue();
            } else if (metricName.compareTo("Service-pd_gb_seconds") == 0) {
              totalDiskTime = ((BigDecimal) metric.getScalar()).longValue();
              diskType = HDD;
            } else if (metricName.compareTo("Service-pd_ssd_gb_seconds") == 0) {
              totalDiskTime = ((BigDecimal) metric.getScalar()).longValue();
              diskType = SSD;
            } else if (metricName.compareTo("CurrentVcpuCount") == 0) {
              currentVcpuCount = ((BigDecimal) metric.getScalar()).doubleValue();
            } else if (metricName.compareTo("TotalStreamingDataProcessed") == 0) {
              totalStreamingData = ((BigDecimal) metric.getScalar()).longValue();
              enableStreamingEngine = true;
            }
          }
        }

        
    }
}