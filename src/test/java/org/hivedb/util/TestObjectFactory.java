package org.hivedb.util;

import java.sql.Date;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.hivedb.management.quartz.MigrationEstimator;
import org.hivedb.management.statistics.NodeStatistics;
import org.hivedb.management.statistics.PartitionKeyStatistics;
import org.hivedb.meta.Access;
import org.hivedb.meta.AccessType;
import org.hivedb.meta.ColumnInfo;
import org.hivedb.meta.Node;
import org.hivedb.meta.NodeGroup;
import org.hivedb.meta.PartitionDimension;
import org.hivedb.meta.Resource;
import org.hivedb.meta.SecondaryIndex;

public class TestObjectFactory {
	public static NodeStatistics filledNodeStatistics(double capacity, List<PartitionKeyStatistics> stats) {
		NodeStatistics s = new NodeStatistics(node(), stats, halfFullEstimator());
		s.getNode().setCapacity(capacity);
		return s;
	}
	
	public static Node node() {
		return new Node("aNode" + new Random().nextInt(), new Access(AccessType.ReadWrite, 0, 0), false);
	}
	
	public static PartitionKeyStatistics partitionKeyStats(int fill){
		PartitionKeyStatistics key = new PartitionKeyStatistics(null, new Random().nextInt(), new Date(System.currentTimeMillis()));
		key.setChildRecordCount(fill);
		return key;
	}
	
	public static PartitionDimension partitionDimension() {
		return new PartitionDimension("aDimension", Types.INTEGER);
	}
	
	public static PartitionDimension partitionDimension(int columnType, NodeGroup nodeGroup, String uri, Collection<Resource> resources) {
		return new PartitionDimension(new Random().nextInt(), "aDimension", columnType, nodeGroup, uri, resources);
	}
	
	public static MigrationEstimator halfFullEstimator() {
		return new MigrationEstimator() {

			public long estimateMoveTime(PartitionKeyStatistics keyStats) {
				return 0;
			}

			public double estimateSize(PartitionKeyStatistics keyStats) {
				return keyStats.getChildRecordCount();
			}

			public double howMuchDoINeedToMove(NodeStatistics stats) {
				return stats.getFillLevel() > stats.getCapacity()/2 ? 
						stats.getFillLevel() - stats.getCapacity()/2 
						: 0;
			}
		};
	}
	
	public static Resource resource() {
		return new Resource(new Random().nextInt(), "aResource", new ArrayList<SecondaryIndex>());
	}
	
	public static SecondaryIndex secondaryIndex(String name) {
		return new SecondaryIndex(new ColumnInfo(name, Types.INTEGER));
	}
}