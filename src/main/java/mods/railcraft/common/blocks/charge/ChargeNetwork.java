/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.blocks.charge;

import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Iterators;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by CovertJaguar on 7/23/2016 for Railcraft.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class ChargeNetwork implements IChargeNetwork {
    private final ChargeGraph NULL_GRAPH = new NullGraph();
    private final Map<BlockPos, ChargeNode> chargeNodes = new HashMap<>();
    private final Map<BlockPos, ChargeNode> chargeQueue = new LinkedHashMap<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Set<ChargeNode> tickingNodes = new LinkedHashSet<>();
    private final Set<ChargeGraph> chargeGraphs = Collections.newSetFromMap(new WeakHashMap<>());
    private final ChargeNode NULL_NODE = new NullNode();
    private final WeakReference<World> world;
    private final BatterySaveData batterySaveData;

    public ChargeNetwork(World world) {
        this.world = new WeakReference<>(world);
        this.batterySaveData = BatterySaveData.forWorld(world);
    }

    private void printDebug(String msg, Object... args) {
        if (RailcraftConfig.printChargeDebug())
            Game.log(Level.INFO, msg, args);
    }

    public void tick() {
        World worldObj = world.get();
        if (worldObj == null)
            return;
        tickingNodes.removeIf(ChargeNode::checkUsageRecordingCompletion);

        // Process the queue of nodes waiting to be added/removed from the network
        Map<BlockPos, ChargeNode> added = new LinkedHashMap<>();
        Iterator<Map.Entry<BlockPos, ChargeNode>> iterator = chargeQueue.entrySet().iterator();
        int count = 0;
        while (iterator.hasNext() && count < 500) {
            count++;
            Map.Entry<BlockPos, ChargeNode> action = iterator.next();
            if (action.getValue() == null) {
                removeNodeImpl(action.getKey());
            } else {
                addNodeImpl(action.getKey(), action.getValue());
                added.put(action.getKey(), action.getValue());
            }
            iterator.remove();
        }

        // Search for connected nodes of recently added nodes and register them too
        // helps fill out the graph faster and more reliably
        Set<BlockPos> newNodes = new HashSet<>();
        for (Map.Entry<BlockPos, ChargeNode> addedNode : added.entrySet()) {
            forConnections(worldObj, addedNode.getKey(), (conPos, conDef) -> {
                if (addNode(worldObj, conPos, conDef))
                    newNodes.add(conPos);
            });
            if (addedNode.getValue().isGraphNull())
                addedNode.getValue().constructGraph();
        }

        // Remove discarded grids and tick what's left
        chargeGraphs.removeIf(g -> g.invalid);
        chargeGraphs.forEach(ChargeGraph::tick);

        if (!newNodes.isEmpty())
            printDebug("Nodes queued: {0}", newNodes.size());
    }

    private void forConnections(World world, BlockPos pos, BiConsumer<BlockPos, IChargeBlock.ChargeDef> action) {
        IBlockState state = WorldPlugin.getBlockState(world, pos);
        if (state.getBlock() instanceof IChargeBlock) {
            IChargeBlock block = (IChargeBlock) state.getBlock();
            IChargeBlock.ChargeDef chargeDef = block.getChargeDef(state, world, pos);
            if (chargeDef != null) {
                Map<BlockPos, EnumSet<IChargeBlock.ConnectType>> possibleConnections = chargeDef.getConnectType().getPossibleConnectionLocations(pos);
                for (Map.Entry<BlockPos, EnumSet<IChargeBlock.ConnectType>> connection : possibleConnections.entrySet()) {
                    IBlockState otherState = WorldPlugin.getBlockState(world, connection.getKey());
                    if (otherState.getBlock() instanceof IChargeBlock) {
                        IChargeBlock.ChargeDef other = ((IChargeBlock) otherState.getBlock()).getChargeDef(WorldPlugin.getBlockState(world, connection.getKey()), world, connection.getKey());
                        if (other != null && other.getConnectType().getPossibleConnectionLocations(connection.getKey()).get(pos).contains(chargeDef.getConnectType())) {
                            action.accept(connection.getKey(), other);
                        }
                    }
                }
            }
        }
    }

    /**
     * Add the node to the network and clean up any node that used to exist there
     */
    private void addNodeImpl(BlockPos pos, ChargeNode node) {
        ChargeNode oldNode = chargeNodes.put(pos, node);

        // update the battery in the save data tracker
        if (node.chargeBattery != null)
            batterySaveData.initBattery(pos, node.chargeBattery);
        else
            batterySaveData.removeBattery(pos);

        // clean up any preexisting node
        if (oldNode != null) {
            oldNode.invalid = true;
            if (oldNode.chargeGraph.isActive()) {
                node.chargeGraph = oldNode.chargeGraph;
                node.chargeGraph.add(node);
            }
            oldNode.chargeGraph = NULL_GRAPH;
        }
    }

    private void removeNodeImpl(BlockPos pos) {
        ChargeNode chargeNode = chargeNodes.remove(pos);
        if (chargeNode != null) {
            chargeNode.invalid = true;
            chargeNode.chargeGraph.destroy(true);
        }
        batterySaveData.removeBattery(pos);
    }

    @Override
    public boolean addNode(World world, BlockPos pos, IChargeBlock.ChargeDef chargeDef) {
        if (!nodeMatches(pos, chargeDef)) {
            printDebug("Registering Node: {0}->{1}", pos, chargeDef);
            chargeQueue.put(pos, new ChargeNode(pos, chargeDef, chargeDef.getBattery(world, pos)));
            return true;
        }
        return false;
    }

    @Override
    public void removeNode(BlockPos pos) {
        chargeQueue.put(pos, null);
    }

    public boolean isUndefined(BlockPos pos) {
        ChargeNode chargeNode = chargeNodes.get(pos);
        return chargeNode == null || chargeNode.isGraphNull() || !chargeQueue.containsKey(pos);
    }

    public boolean isReady(BlockPos pos) {
        ChargeNode chargeNode = chargeNodes.get(pos);
        return chargeNode != null && !chargeNode.isGraphNull();
    }

    @Override
    public ChargeGraph grid(BlockPos pos) {
        return access(pos).getGrid();
    }

    @Override
    public ChargeNode access(BlockPos pos) {
        ChargeNode node = chargeNodes.get(pos);
        if (node != null && node.invalid) {
            removeNodeImpl(pos);
            node = null;
        }
        if (node == null) {
            World worldObj = world.get();
            if (worldObj != null) {
                IBlockState state = WorldPlugin.getBlockState(worldObj, pos);
                if (state.getBlock() instanceof IChargeBlock) {
                    IChargeBlock.ChargeDef chargeDef = ((IChargeBlock) state.getBlock()).getChargeDef(state, worldObj, pos);
                    if (chargeDef != null) {
                        node = new ChargeNode(pos, chargeDef, chargeDef.getBattery(worldObj, pos));
                        addNodeImpl(pos, node);
                        if (node.isGraphNull())
                            node.constructGraph();
                    }
                }
            }
        }
        return node == null ? NULL_NODE : node;
    }

    public ChargeNode getNodeSafe(BlockPos pos) {
        ChargeNode node = chargeNodes.get(pos);
        return node == null ? NULL_NODE : node;
    }

    public boolean nodeMatches(BlockPos pos, IChargeBlock.ChargeDef chargeDef) {
        ChargeNode node = chargeNodes.get(pos);
        return node != null && node.isValid() && node.chargeDef == chargeDef;
    }

    @Override
    public IChargeBlock.ChargeBattery makeBattery(BlockPos pos, Supplier<IChargeBlock.ChargeBattery> supplier) {
        ChargeNetwork.ChargeNode node = getNodeSafe(pos);
        IChargeBlock.ChargeBattery battery = node.getBattery();
        return battery == null ? supplier.get() : battery;
    }

    public class ChargeGraph extends ForwardingSet<ChargeNode> {
        private final Set<ChargeNode> chargeNodes = new HashSet<>();
        private final Map<ChargeNode, IChargeBlock.ChargeBattery> chargeBatteries = new LinkedHashMap<>();
        private boolean invalid;
        private double totalMaintenanceCost;
        private double chargeUsedThisTick;
        private double averageUsagePerTick;

        @Override
        protected Set<ChargeNode> delegate() {
            return chargeNodes;
        }

        @Override
        public boolean add(ChargeNode chargeNode) {
            boolean added = super.add(chargeNode);
            if (added)
                totalMaintenanceCost += chargeNode.chargeDef.getMaintenanceCost();
            chargeNode.chargeGraph = this;
            if (chargeNode.chargeBattery != null)
                chargeBatteries.put(chargeNode, chargeNode.chargeBattery);
            else {
                chargeBatteries.remove(chargeNode);
                batterySaveData.removeBattery(chargeNode.pos);
            }
            return added;
        }

        @Override
        public boolean addAll(Collection<? extends ChargeNode> collection) {
            return standardAddAll(collection);
        }

        @Override
        public boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super ChargeNode> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<ChargeNode> iterator() {
            return Iterators.unmodifiableIterator(super.iterator());
        }

        private void destroy(boolean touchNodes) {
            if (isActive()) {
                printDebug("Destroying graph: {0}", this);
                invalid = true;
                totalMaintenanceCost = 0.0;
                if (touchNodes) {
                    forEach(n -> n.chargeGraph = NULL_GRAPH);
                }
                chargeBatteries.clear();
                super.clear();
                chargeGraphs.remove(this);
            }
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        private void tick() {
            removeCharge(totalMaintenanceCost);

            // balance the charge in all the batteries in the graph
            double capacity = getCapacity();
            if (capacity > 0.0) {
                final double chargeLevel = getCharge() / capacity;
                chargeBatteries.forEach((node, bat) -> {
                    bat.setCharge(chargeLevel * bat.getCapacity());
                    batterySaveData.updateBatteryRecord(node.pos, bat);
                });
            }

            // track usage patterns
            averageUsagePerTick = (averageUsagePerTick * 49D + chargeUsedThisTick) / 50D;
            chargeUsedThisTick = 0.0;
        }

        public double getCharge() {
            return chargeBatteries.values().stream().mapToDouble(IChargeBlock.ChargeBattery::getCharge).sum();
        }

        public double getCapacity() {
            return chargeBatteries.values().stream().mapToDouble(IChargeBlock.ChargeBattery::getCapacity).sum();
        }

        public double getMaxNetworkDraw() {
            return chargeBatteries.values().stream().mapToDouble(IChargeBlock.ChargeBattery::getAvailableCharge).sum();
        }

        public double getNetworkEfficiency() {
            return chargeBatteries.values().stream().mapToDouble(IChargeBlock.ChargeBattery::getEfficiency).average().orElse(1.0);
        }

        public int getComparatorOutput() {
            double level = getCharge() / getCapacity();
            return Math.round((float) (15.0 * level));
        }

        public double getMaintenanceCost() {
            return totalMaintenanceCost;
        }

        public double getAverageUsagePerTick() {
            return averageUsagePerTick;
        }

        public double getUsageRatio() {
            if (isInfinite())
                return 0.0;
            double maxDraw = getMaxNetworkDraw();
            if (maxDraw <= 0.0)
                return 1.0;
            return Math.min(getAverageUsagePerTick() / maxDraw, 1.0);
        }

        public boolean isInfinite() {
            return chargeBatteries.values().stream().anyMatch(IChargeBlock.ChargeBattery::isInfinite);
        }

        public boolean isActive() {
            return !isNull();
        }

        public boolean isNull() {
            return false;
        }

        /**
         * Remove the requested amount of charge if possible and
         * return whether sufficient charge was available to perform the operation.
         *
         * @return true if charge could be removed in full
         */
        public boolean useCharge(double amount) {
            double efficiency = getNetworkEfficiency();
            if (getMaxNetworkDraw() >= amount / efficiency) {
                removeCharge(amount, efficiency);
                return true;
            }
            return false;
        }

        /**
         * Determines if the grid is capable of providing the required charge in a single operation.
         * The amount of charge that can be withdraw from the grid is dependant on the overall efficiency
         * of the grid and its available charge.
         *
         * @param amount the requested charge
         * @return true if the grid can provide the power
         */
        public boolean hasCapacity(double amount) {
            return getMaxNetworkDraw() >= amount / getNetworkEfficiency();
        }

        /**
         * Remove up to the requested amount of charge and returns the amount
         * removed.
         *
         * @return charge removed
         */
        public double removeCharge(double desiredAmount) {
            return removeCharge(desiredAmount, getNetworkEfficiency());
        }

        /**
         * Remove up to the requested amount of charge and returns the amount
         * removed.
         *
         * @return charge removed
         */
        private double removeCharge(double desiredAmount, double efficiency) {
            final double amountToDraw = desiredAmount / efficiency;
            double amountNeeded = amountToDraw;
            for (Map.Entry<ChargeNode, IChargeBlock.ChargeBattery> battery : chargeBatteries.entrySet()) {
                amountNeeded -= battery.getValue().removeCharge(amountNeeded);
                batterySaveData.updateBatteryRecord(battery.getKey().pos, battery.getValue());
                if (amountNeeded <= 0.0)
                    break;
            }
            double chargeRemoved = amountToDraw - amountNeeded;
            chargeUsedThisTick += chargeRemoved;
            return chargeRemoved * efficiency;
        }

        @Override
        public String toString() {
            return String.format("ChargeGraph{s=%d,b=%d}", size(), chargeBatteries.size());
        }
    }

    private class NullGraph extends ChargeGraph {
        @Override
        protected Set<ChargeNode> delegate() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public String toString() {
            return "ChargeGraph{NullGraph}";
        }
    }

    private class UsageRecorder {
        private final int ticksToRecord;
        private final Consumer<Double> usageConsumer;

        private double chargeUsed;
        private int ticksRecorded;

        public UsageRecorder(int ticksToRecord, Consumer<Double> usageConsumer) {
            this.ticksToRecord = ticksToRecord;
            this.usageConsumer = usageConsumer;
        }

        public void useCharge(double amount) {
            chargeUsed += amount;
        }

        public Boolean checkCompletion() {
            ticksRecorded++;
            if (ticksRecorded > ticksToRecord) {
                usageConsumer.accept(chargeUsed / ticksToRecord);
                return true;
            }
            return false;
        }
    }

    public class ChargeNode {
        protected final @Nullable IChargeBlock.ChargeBattery chargeBattery;
        private final BlockPos pos;
        private final IChargeBlock.ChargeDef chargeDef;
        private ChargeGraph chargeGraph = NULL_GRAPH;
        private boolean invalid;
        private Optional<UsageRecorder> usageRecorder = Optional.empty();
        private final Collection<BiConsumer<ChargeNode, Double>> listeners = new LinkedHashSet<>();

        private ChargeNode(BlockPos pos, IChargeBlock.ChargeDef chargeDef, @Nullable IChargeBlock.ChargeBattery chargeBattery) {
            this.pos = pos;
            this.chargeDef = chargeDef;
            this.chargeBattery = chargeBattery;
        }

        public IChargeBlock.ChargeDef getChargeDef() {
            return chargeDef;
        }

        private void forConnections(Consumer<ChargeNode> action) {
            Map<BlockPos, EnumSet<IChargeBlock.ConnectType>> possibleConnections = chargeDef.getConnectType().getPossibleConnectionLocations(pos);
            for (Map.Entry<BlockPos, EnumSet<IChargeBlock.ConnectType>> connection : possibleConnections.entrySet()) {
                ChargeNode other = chargeNodes.get(connection.getKey());
                if (other != null && connection.getValue().contains(other.chargeDef.getConnectType())
                        && other.chargeDef.getConnectType().getPossibleConnectionLocations(connection.getKey()).get(pos).contains(chargeDef.getConnectType())) {
                    action.accept(other);
                }
            }
        }

        public ChargeGraph getGrid() {
            if (chargeGraph.isActive())
                return chargeGraph;
            constructGraph();
            return chargeGraph;
        }

        public void addListener(BiConsumer<ChargeNode, Double> listener) {
            listeners.add(listener);
        }

        public void removeListener(BiConsumer<ChargeNode, Double> listener) {
            listeners.remove(listener);
        }

        public void startUsageRecording(int ticksToRecord, Consumer<Double> usageConsumer) {
            usageRecorder = Optional.of(new UsageRecorder(ticksToRecord, usageConsumer));
            tickingNodes.add(this);
        }

        public boolean checkUsageRecordingCompletion() {
            usageRecorder = usageRecorder.filter(UsageRecorder::checkCompletion);
            return !usageRecorder.isPresent();
        }

        public boolean hasCapacity(double amount) {
            return chargeGraph.hasCapacity(amount);
        }

        /**
         * Remove the requested amount of charge if possible and
         * return whether sufficient charge was available to perform the operation.
         *
         * @return true if charge could be removed in full
         */
        public boolean useCharge(double amount) {
            boolean removed = chargeGraph.useCharge(amount);
            if (removed) {
                listeners.forEach(c -> c.accept(this, amount));
                usageRecorder.ifPresent(r -> r.useCharge(amount));
            }
            return removed;
        }

        /**
         * @return amount removed, may be less than desiredAmount
         */
        public double removeCharge(double desiredAmount) {
            double removed = chargeGraph.removeCharge(desiredAmount);
            listeners.forEach(c -> c.accept(this, removed));
            usageRecorder.ifPresent(r -> r.useCharge(removed));
            return removed;
        }

        public boolean isValid() {
            return !invalid;
        }

        public boolean isGraphNull() {
            return chargeGraph.isNull();
        }

        protected void constructGraph() {
            Set<ChargeNode> visitedNodes = new HashSet<>();
            visitedNodes.add(this);
            Set<ChargeNode> nullNodes = new HashSet<>();
            nullNodes.add(this);
            Deque<ChargeNode> nodeQueue = new ArrayDeque<>();
            nodeQueue.add(this);
            TreeSet<ChargeGraph> graphs = new TreeSet<>(Comparator.comparingInt(ForwardingCollection::size));
            graphs.add(chargeGraph);
            ChargeNode nextNode;
            while ((nextNode = nodeQueue.poll()) != null) {
                nextNode.forConnections(n -> {
                    if (!visitedNodes.contains(n) && (n.isGraphNull() || !graphs.contains(n.chargeGraph))) {
                        if (n.isGraphNull())
                            nullNodes.add(n);
                        graphs.add(n.chargeGraph);
                        visitedNodes.add(n);
                        nodeQueue.addLast(n);
                    }
                });
            }
            chargeGraph = Objects.requireNonNull(graphs.pollLast());
            if (chargeGraph.isNull() && nullNodes.size() > 1) {
                chargeGraph = new ChargeGraph();
                chargeGraphs.add(chargeGraph);
            }
            if (chargeGraph.isActive()) {
                int originalSize = chargeGraph.size();
                chargeGraph.addAll(nullNodes);
                for (ChargeGraph graph : graphs) {
                    chargeGraph.addAll(graph);
                }
                graphs.forEach(g -> g.destroy(false));
                printDebug("Constructing Graph: {0}->{1} Added {2} nodes", pos, chargeGraph, chargeGraph.size() - originalSize);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ChargeNode that = (ChargeNode) o;

            return pos.equals(that.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }

        public @Nullable IChargeBlock.ChargeBattery getBattery() {
            return chargeBattery;
        }

        @Override
        public String toString() {
            return String.format("ChargeNode{%s|%s}", pos, chargeDef);
        }
    }

    public class NullNode extends ChargeNode {
        public NullNode() {
            super(new BlockPos(0, 0, 0), new IChargeBlock.ChargeDef(IChargeBlock.ConnectType.BLOCK, 0.0), null);
        }

        @Override
        public @Nullable IChargeBlock.ChargeBattery getBattery() {
            return null;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public ChargeGraph getGrid() {
            return NULL_GRAPH;
        }

        @Override
        public boolean hasCapacity(double amount) {
            return false;
        }

        @Override
        public boolean useCharge(double amount) {
            return false;
        }

        @Override
        public double removeCharge(double desiredAmount) {
            return 0.0;
        }

        @Override
        protected void constructGraph() {
        }

        @Override
        public String toString() {
            return "ChargeNode{NullNode}";
        }
    }
}
