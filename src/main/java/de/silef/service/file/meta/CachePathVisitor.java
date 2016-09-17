package de.silef.service.file.meta;

import de.silef.service.file.util.PathVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebastian on 17.09.16.
 */
public class CachePathVisitor extends PathVisitor {

    private List<FileMeta> parentStack;

    private FileMeta root;

    public CachePathVisitor() throws IOException {
        parentStack = new ArrayList<>();
    }

    public FileMeta getRoot() {
        return root;
    }

    @Override
    public VisitorResult preVisitDirectory(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            return VisitorResult.SKIP;
        }
        FileMeta parent = parentStack.isEmpty() ? null : parentStack.get(parentStack.size() - 1);
        parentStack.add(new FileMeta(parent, path));
        return super.preVisitDirectory(path);
    }

    @Override
    public VisitorResult visitFile(Path path) throws IOException {
        if (Files.isReadable(path)) {
            addCacheItem(path);
        }
        return super.visitFile(path);
    }

    @Override
    public VisitorResult postVisitDirectory(Path dir) throws IOException {
        root = parentStack.remove(parentStack.size() - 1);
        return super.postVisitDirectory(dir);
    }


    private void addCacheItem(Path path) throws IOException {
        FileMeta parent = parentStack.get(parentStack.size() - 1);
        new FileMeta(parent, path);
    }

}
