package net.runelite.client.plugins.microbot.TaF.AutoGauntlet.ResourceData;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.runelite.api.GameObject;
import net.runelite.api.coords.LocalPoint;

import java.util.Collection;
import java.util.Set;

public class ResourceNodes {
    private final Multimap<String, GameObject> resourceNodes;

    public ResourceNodes() {
        this.resourceNodes = ArrayListMultimap.create();
    }

    public void addResourceNode(GameObject node) {
        String resourceName = GetResourceName(node);
        if (resourceName == null) {
            return;
        }

        // Check if the node already exists in the map based on ID and location
        for (GameObject existingNode : this.resourceNodes.get(resourceName)) {
            if (equals(existingNode, node)) {
                // Node already exists, don't add it again
                return;
            }
        }
        // Node doesn't exist yet, so add it
        this.resourceNodes.put(resourceName, node);
    }

    private String GetResourceName(GameObject gameObject) {
        return "";
    }

    public Collection<GameObject> getResourceNodes(String name) {
        return this.resourceNodes.get(name);
    }

    public GameObject getClosestResourceNode(String resourceName, LocalPoint localPoint) {
        GameObject closestNode = null;
        int closestDistance = Integer.MAX_VALUE;

        for (GameObject node : this.resourceNodes.get(resourceName)) {
            int distance = node.getLocalLocation().distanceTo(localPoint);
            if (distance < closestDistance) {
                closestNode = node;
                closestDistance = distance;
            }
        }

        return closestNode;
    }

    // Clear all resource nodes
    public void clearResourceNodes() {
        this.resourceNodes.clear();
    }

    // Get all resource types
    public Set<String> getResourceTypes() {
        return this.resourceNodes.keySet();
    }

    // Get count of resources by type
    public int getResourceCount(String resourceName) {
        return this.resourceNodes.get(resourceName).size();
    }

    public boolean equals(GameObject first, GameObject second) {
        return first.getId() == second.getId() && first.getLocalLocation().equals(second.getLocalLocation());
    }
}