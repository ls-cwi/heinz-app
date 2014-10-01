package org.cytoscape.heinz.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.group.CyGroupManager;

/**
 * A few methods used in several Tasks to work with CyTables.
 */
public class TableUtils {
	
	public static <T> T getNodeAttribute(
			CyNode node,
			CyNetwork network,
			String columnName,
			Class<? extends T> type,
			CyGroupManager groupManager) {
		// check if the node has a row in the node table
		if (!network.getDefaultNodeTable().rowExists(node.getSUID())) {
			throw new IllegalStateException(
					"Node " +
					node.getSUID() + 
					" not found in the node table.");
		}
		// get the node’s table row
		CyRow ntRow = network.getDefaultNodeTable().getRow(node.getSUID());
		// check for collapsed CyGroups
		if (groupManager.isGroup(node, network)) {
			throw new IllegalArgumentException(
					"Node " +
					ntRow.get("name", String.class) +
					" is a collapsed group; Heinz does not support this.");
		}
		// test if a p-value exists for this row
		if (!ntRow.isSet(columnName)) {
			throw new IllegalArgumentException(
					columnName +
					" value for node ‘" +
					ntRow.get("name", String.class) +
					"’ missing.");
		}
		return ntRow.get(columnName, type);
	}
}
