package org.sunbird.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class CassandraConnection {
    private static Logger logger = Logger.getLogger(CassandraConnection.class);

    private Cluster cluster;

    private Session session;

    private static CassandraConnection instance;

    public static CassandraConnection getInstance() {
        if (null == instance) {
            instance = new CassandraConnection();
            return instance;
        } else {
            return instance;
        }
    }

    private CassandraConnection() {
        connect(System.getenv("cassandra_host"));
    }

    public void connect(String nodes) {
        String[] hosts =  null;
        if (StringUtils.isNotBlank(nodes)) {
            hosts =  nodes.split(",");
        } else {
            hosts = new String[] { "localhost" };
        }
        cluster = Cluster.builder().addContactPoints(hosts)
                .withRetryPolicy(DefaultRetryPolicy.INSTANCE).build();
        session = cluster.connect();
    }

    public Session getSession() {
        return this.session;
    }

    public void close() {
        session.close();
        cluster.close();
    }
}
