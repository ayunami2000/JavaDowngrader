/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.javadowngrader.standalone.transform;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import java.io.IOException;
import java.nio.file.FileSystem;

public class ClosingFileSystemClassProvider extends PathClassProvider implements AutoCloseable {
    private final FileSystem fs;

    public ClosingFileSystemClassProvider(FileSystem fs, IClassProvider parent) {
        super(fs.getRootDirectories().iterator().next(), parent);
        this.fs = fs;
    }

    @Override
    public void close() throws IOException {
        fs.close();
    }
}
