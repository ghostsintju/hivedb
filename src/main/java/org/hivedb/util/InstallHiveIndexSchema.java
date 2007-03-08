package org.hivedb.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.hivedb.HiveException;
import org.hivedb.meta.ColumnInfo;
import org.hivedb.meta.Hive;
import org.hivedb.meta.Node;
import org.hivedb.meta.NodeGroup;
import org.hivedb.meta.PartitionDimension;
import org.hivedb.meta.Resource;
import org.hivedb.meta.SecondaryIndex;
import org.hivedb.util.scenarioBuilder.HiveScenarioConfig;
import org.hivedb.util.scenarioBuilder.PrimaryIndexIdentifiable;
import org.hivedb.util.scenarioBuilder.RingIteratorable;
import org.hivedb.util.scenarioBuilder.SecondaryIndexIdentifiable;
import org.hivedb.util.scenarioBuilder.Transform;
import org.hivedb.util.scenarioBuilder.Unary;

public class InstallHiveIndexSchema {
	public static Map<Class, PartitionDimension> install(
			final HiveScenarioConfig hiveScenarioConfig,
			final Hive hive) throws HiveException {
		
		final RingIteratorable<String> indexUriIterator = new RingIteratorable<String>(hiveScenarioConfig.getIndexUris(hive));
	
		// Create partition dimensions and their its subordinate NodeGroup, primary Node, Resources, and SecondaryIndexes
		Map<Class, PartitionDimension> partitionDimensionMap = Transform.toMap(
			new Transform.IdentityFunction<Class>(),
			new Unary<Class, PartitionDimension>() {
				public PartitionDimension f(final Class primaryClass) {
					try {
						final PrimaryIndexIdentifiable primaryInstancePrototype = getPrimaryIndexIdentifiablePrototype(primaryClass); 
						return new PartitionDimension(
							primaryInstancePrototype.getPartitionDimensionName(),
							JdbcTypeMapper.primitiveTypeToJdbcType(primaryClass.getMethod("getIdAsPrimaryIndexInstance").getReturnType()),
							new NodeGroup(Transform.map(new Unary<Node,Node>() { public Node f(Node n) {return new Node(n.getUri(), n.isReadOnly());}}, hiveScenarioConfig.getNodes(hive))),
							indexUriIterator.next(),
							Transform.map(new Unary<Class, Resource>() {
								public Resource f(Class secondaryClass) { 
									SecondaryIndexIdentifiable secondaryIndexIdentifiable = getSecondaryIndexIdentifiablePrototype(primaryClass, secondaryClass);									
									return new Resource(secondaryIndexIdentifiable.getResourceName(), constructSecondaryIndexesOfResource(primaryClass, secondaryClass));
								}},
								hiveScenarioConfig.getPrimaryToResourceMap().get(primaryClass))
						);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}},
				Arrays.asList(hiveScenarioConfig.getPrimaryClasses()));
		
		for (PartitionDimension partitionDimension : partitionDimensionMap.values())
			hive.addPartitionDimension(partitionDimension);
		
		try {
			hive.create();
		}
		catch (Exception exception)
		{
			throw new RuntimeException(exception);
		}
		return partitionDimensionMap;
	}
	
	public static Collection<SecondaryIndex> constructSecondaryIndexesOfResource(Class primaryIndexClass, Class secondaryIndexClass) {	
		try {
			return 
				Arrays.asList(new SecondaryIndex[] {			
					new SecondaryIndex(
						new ColumnInfo(
							getNameOfSecondaryIndex(primaryIndexClass, secondaryIndexClass),											
							JdbcTypeMapper.primitiveTypeToJdbcType(secondaryIndexClass.getMethod("getIdAsSecondaryIndexInstance").getReturnType())))		
				});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	private static String getNameOfSecondaryIndex(Class primaryIndexClass, Class secondaryIndexClass) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		Object newInstance = constructSecondaryInstance(primaryIndexClass, secondaryIndexClass);
		return (String)secondaryIndexClass.getMethod("getSecondaryIdName").invoke(newInstance);					
	}

	public static PrimaryIndexIdentifiable getPrimaryIndexIdentifiablePrototype(final Class primaryClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		return (PrimaryIndexIdentifiable) primaryClass.getConstructor(new Class[] {}).newInstance(new Object[] {});
	}
	public static SecondaryIndexIdentifiable getSecondaryIndexIdentifiablePrototype(final Class primaryClass, final Class secondaryClass) {
		return constructSecondaryInstance(primaryClass, secondaryClass);
	}
	
	/**
	 *  Constructs a SecondaryIndexInstance, taking into account whethter the secondary index class
	 *  is also a primary index class (i.e. is a PrimaryAndSecondaryIndexIdentifiable)
	 *  PrimaryAndSecondaryIndexIdentifiable classes construct with no arguments, since their secondary
	 *  index is a reference from a field of the instances to its own id
	 *  SecondaryIndexIdentifiable classes construct with a PrimaryIndexIdentifiable instance, whose
	 *  id becomes the primary index key of the secondary index key
	 * @param primaryIndexClass
	 * @param secondaryIndexClass
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static SecondaryIndexIdentifiable constructSecondaryInstance(Class primaryIndexClass, Class secondaryIndexClass) {
		try {
			// Choose the right constructor depending on whether the secondaryIndexClass is the same as the primary index class
			Constructor constructor = secondaryIndexClass.equals(primaryIndexClass)
				? secondaryIndexClass.getConstructor(new Class[] {})
				: secondaryIndexClass.getConstructor(new Class[] {primaryIndexClass});
			Object[] args = secondaryIndexClass.equals(primaryIndexClass)
				? new Object[] {}
				: new Object[] { primaryIndexClass.getConstructor(new Class[] {}).newInstance(new Object[] {}) };			
			SecondaryIndexIdentifiable newInstance = (SecondaryIndexIdentifiable) constructor.newInstance(args);
			return newInstance;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	
}