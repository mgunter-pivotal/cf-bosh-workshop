package org.cloudfoundry.community.servicebroker.postgresql.repository;

import org.cloudfoundry.community.servicebroker.postgresql.model.ServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mwright on 4/18/16.
 */
public interface PostgresServiceInstanceRepository extends JpaRepository<ServiceInstance, String> {
}

