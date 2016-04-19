/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cloudfoundry.community.servicebroker.postgresql.service;

import org.cloudfoundry.community.servicebroker.postgresql.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.postgresql.repository.PostgresServiceInstanceRepository;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class PostgreSQLServiceInstanceService implements ServiceInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLServiceInstanceService.class);

    private final Role role;

    private final DatabaseAdmin dbAdmin;

    private final PostgresServiceInstanceRepository repos;

    @Autowired
    public PostgreSQLServiceInstanceService(DatabaseAdmin dbAdmin, Role role, PostgresServiceInstanceRepository repos) {
        this.role = role;
        this.repos = repos;
        this.dbAdmin = dbAdmin;
    }

    @Override
    public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest)
            throws ServiceInstanceExistsException, ServiceBrokerException {
        String serviceInstanceId = createServiceInstanceRequest.getServiceInstanceId();
        String serviceId = createServiceInstanceRequest.getServiceDefinitionId();
        String planId = createServiceInstanceRequest.getPlanId();
        String organizationGuid = createServiceInstanceRequest.getOrganizationGuid();
        String spaceGuid = createServiceInstanceRequest.getSpaceGuid();
        try {
            dbAdmin.createDatabaseForInstance(serviceInstanceId, serviceId, planId, organizationGuid, spaceGuid);
            role.createRoleForInstance(serviceInstanceId);
        } catch (SQLException e) {
            logger.error("Error while creating service instance '" + serviceInstanceId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }
        ServiceInstance instance =  new ServiceInstance(createServiceInstanceRequest);
        repos.save(instance);

        return new CreateServiceInstanceResponse();
    }

    @Override
    public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest getLastServiceOperationRequest) {
        return new GetLastServiceOperationResponse().withOperationState(OperationState.SUCCEEDED);
    }

    @Override
    public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest)
            throws ServiceBrokerException {
        String serviceInstanceId = deleteServiceInstanceRequest.getServiceInstanceId();
        ServiceInstance instance = getServiceInstance(serviceInstanceId);

        try {
            dbAdmin.deleteDatabase(serviceInstanceId);
            role.deleteRole(serviceInstanceId);
        } catch (SQLException e) {
            logger.error("Error while deleting service instance '" + serviceInstanceId + "'", e);
            throw new ServiceBrokerException(e.getMessage());
        }

        repos.delete(serviceInstanceId);
        return new DeleteServiceInstanceResponse();
    }

    @Override
    public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest updateServiceInstanceRequest)
            throws ServiceInstanceUpdateNotSupportedException, ServiceBrokerException, ServiceInstanceDoesNotExistException {
        String instanceId = updateServiceInstanceRequest.getServiceInstanceId();
        ServiceInstance instance = repos.findOne(instanceId);
        if (instance == null) {
            throw new ServiceInstanceDoesNotExistException(instanceId);
        }

        repos.delete(instanceId);
        ServiceInstance updatedInstance = new ServiceInstance(updateServiceInstanceRequest);
        repos.save(updatedInstance);
        return new UpdateServiceInstanceResponse();
    }

    public ServiceInstance getServiceInstance(String id) {
        return repos.findOne(id);
    }
}