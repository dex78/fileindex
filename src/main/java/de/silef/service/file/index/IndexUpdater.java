package de.silef.service.file.index;

import de.silef.service.file.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by sebastian on 18.09.16.
 */
public class IndexUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(IndexUpdater.class);

    private Path base;

    private IndexNode root;

    public IndexUpdater(Path base, IndexNode root) {
        this.base = base;
        this.root = root;
    }

    public IndexNode getRoot() {
        return root;
    }

    public void update(IndexChange change, Consumer<IndexNode> fileUpdateConsumer, boolean suppressErrors) throws IOException {
        if (!change.hasChanges()) {
            return;
        }
        Set<IndexNode> update = new HashSet<>(change.getCreated());
        update.addAll(new HashSet<>(change.getModified()));

        updateAll(update, fileUpdateConsumer, suppressErrors);
        removeAll(change.getRemoved());

        root.getHash();
    }

    private void updateAll(Collection<IndexNode> nodes, Consumer<IndexNode> fileUpdateConsumer, boolean suppressErrors) throws IOException {
        int updatedFiles = 0;
        long updatedBytes = 0;
        for (IndexNode node : nodes) {
            Path path = node.getRelativePath();
            Path file = base.resolve(path);
            try {
                updatePath(path, fileUpdateConsumer);
                updatedFiles++;
                updatedBytes += Files.size(file);
            } catch (IOException e) {
                if (!suppressErrors) {
                    throw e;
                } else {
                    LOG.info("Could not index file {}", file);
                }
            }
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Updated index with {} files of {}", updatedFiles, ByteUtil.toHumanSize(updatedBytes));
        }
    }

    private void updatePath(Path path, Consumer<IndexNode> fileUpdateConsumer) throws IOException {
        List<String> names = new LinkedList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            names.add(path.getName(i).toString());
        }
        insertNode(root, names, fileUpdateConsumer);
    }

    private void insertNode(IndexNode node, List<String> names, Consumer<IndexNode> fileUpdateConsumer) throws IOException {
        if (node.getMode() != FileMode.DIRECTORY) {
            throw new IllegalArgumentException("node must be an directory");
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Path names must not be empty");
        }

        String name = names.remove(0);

        Path file = base.resolve(node.getRelativePath()).resolve(name);
        if (names.isEmpty()) {
            node.removeChildByName(name);
            IndexNode child = IndexNode.createFromPath(node, file);
            node.addChild(child);
            fileUpdateConsumer.accept(child);
            node.resetHashesToRootNode();
        } else {
            IndexNode child = node.findChildByName(name);
            if (child == null) {
                child = IndexNode.createFromPath(node, file);
                node.addChild(child);
                node.resetHashesToRootNode();
            }
            insertNode(child, names, fileUpdateConsumer);
        }
    }

    private void removeAll(Collection<IndexNode> nodes) {
        LOG.debug("Removing {} files from index", nodes.size());
        for (IndexNode node : nodes) {
            removeNode(node);
        }
        LOG.debug("Removed {} files from index", nodes.size());
    }

    private void removeNode(IndexNode node) {
        Path path = node.getRelativePath();
        List<String> names = new LinkedList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            names.add(path.getName(i).toString());
        }
        removeNode(root, names);
    }

    private void removeNode(IndexNode node, List<String> names) {
        if (node.getMode() != FileMode.DIRECTORY) {
            throw new IllegalArgumentException("node must be an directory");
        }
        String name = names.remove(0);
        if (names.isEmpty()) {
            node.removeChildByName(name);
            node.resetHashesToRootNode();
        } else {
            IndexNode child = node.findChildByName(name);
            if (child != null) {
                removeNode(child, names);
            }
        }
    }

}
