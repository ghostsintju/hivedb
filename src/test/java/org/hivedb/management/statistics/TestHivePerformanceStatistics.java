package org.hivedb.management.statistics;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;

import org.hivedb.Hive;
import org.hivedb.HiveException;
import org.hivedb.management.HiveInstaller;
import org.hivedb.meta.AccessType;
import org.hivedb.meta.IndexSchema;
import org.hivedb.util.database.HiveTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestHivePerformanceStatistics extends HiveTestCase{
	private Hive hive;
	
	@BeforeMethod
	public void setup() throws Exception {
		this.cleanupDbAfterEachTest = true;
		super.beforeMethod();
		new HiveInstaller(getConnectString()).run();
		hive = Hive.load(getConnectString());
		hive.setPerformanceStatistics(new HivePerformanceStatisticsMBean(10000,1000));
		hive.addPartitionDimension(createPopulatedPartitionDimension());
		new IndexSchema(hive.getPartitionDimension(partitionDimensionName())).install();
		hive.addNode(hive.getPartitionDimension(partitionDimensionName()), createNode());
		hive.insertPrimaryIndexKey(partitionDimensionName(), intKey());
	}
	
	@AfterMethod
	public void resetReadOnly() throws HiveException {
		hive.updateHiveReadOnly(false);
	}
	
	@Test
	public void testWriteConnectionTracking() throws Exception{
		Collection<Connection> connections = new ArrayList<Connection>();
		
		for(int i=0; i<5; i++)
			connections.add( hive.getConnection( hive.getPartitionDimension(partitionDimensionName()), intKey(), AccessType.ReadWrite));
		
		Assert.assertEquals(connections.size(), hive.getPerformanceStatistics().getSumNewWriteConnections());
	}
	
	@Test
	public void testReadConnectionTracking() throws Exception{
		Collection<Connection> connections = new ArrayList<Connection>();
		for(int i=0; i<5; i++)
			connections.add( hive.getConnection( hive.getPartitionDimension(partitionDimensionName()), intKey(), AccessType.Read));
		
		Assert.assertEquals(connections.size(), hive.getPerformanceStatistics().getSumNewReadConnections());
	}
	
	@Test
	public void testConnectionFailures() throws Exception {
		hive.updateHiveReadOnly(true);
		Collection<Connection> connections = new ArrayList<Connection>();
		for(int i=0; i<5; i++){
			try {
				connections.add( hive.getConnection( hive.getPartitionDimension(partitionDimensionName()), intKey(), AccessType.ReadWrite));
			} catch( Exception e) {
				//CRUSH! KILL! DESTROY!
			}
		}
		
		Assert.assertEquals(5, hive.getPerformanceStatistics().getSumConnectionFailures());
	}
	
	public Integer intKey() { return new Integer(7); }
}
