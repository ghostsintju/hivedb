package org.hivedb.management.statistics;

import static org.testng.AssertJUnit.fail;

import org.apache.commons.dbcp.BasicDataSource;
import org.hivedb.meta.GlobalSchema;
import org.hivedb.meta.Hive;
import org.hivedb.meta.persistence.HiveBasicDataSource;
import org.hivedb.meta.persistence.HiveSemaphoreDao;
import org.hivedb.util.DerbyTestCase;
import org.hivedb.util.HiveScenario;

public abstract class PirateHiveTestCase extends DerbyTestCase {
	protected HiveScenario yeScenario = null;
	
	public void setUp() {
		try {
			// global schema
			new GlobalSchema(getConnectString()).install();
			BasicDataSource ds = new HiveBasicDataSource(getConnectString());
			new HiveSemaphoreDao(ds).create();
			yeScenario = HiveScenario.buildAndSyncHive(Hive.load(getConnectString()));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unable to initialize the hive: " + e.getMessage());
		} 
	}
}