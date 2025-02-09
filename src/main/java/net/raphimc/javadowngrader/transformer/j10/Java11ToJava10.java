/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.javadowngrader.transformer.j10;

import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j10.methodcallreplacer.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class Java11ToJava10 extends DowngradingTransformer {

    public Java11ToJava10() {
        super(Opcodes.V11, Opcodes.V10);

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isBlank", "()Z", new StringIsBlankMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/lang/String", "strip", "()Ljava/lang/String;", new StringStripMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/lang/String", "stripLeading", "()Ljava/lang/String;", new StringStripLeadingMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/lang/String", "stripTrailing", "()Ljava/lang/String;", new StringStripTrailingMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/nio/file/Files", "readString", new FilesReadStringMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/nio/file/Path", "of", new PathOfMCR());

        final String[] optionalClasses = new String[]{"java/util/Optional", "java/util/OptionalDouble", "java/util/OptionalInt", "java/util/OptionalLong"};
        for (String optionalClass : optionalClasses) {
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, optionalClass, "isEmpty", "()Z", new OptionalIsEmptyMCR(optionalClass));
        }

        this.addMethodCallReplacer(Opcodes.INVOKEINTERFACE, "java/util/List", "toArray", "(Ljava/util/function/IntFunction;)[Ljava/lang/Object;", new ListToArrayMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/lang/Character", "toString", "(I)Ljava/lang/String;", new CharacterToStringMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "setInput", "(Ljava/nio/ByteBuffer;)V", new InflaterSetInputMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "inflate", "(Ljava/nio/ByteBuffer;)I", new InflaterInflateMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/io/OutputStream", "nullOutputStream", "()Ljava/io/OutputStream;", new OutputStreamNullOutputStreamMCR());
    }

    @Override
    protected void preTransform(ClassNode classNode, DowngradeResult result) {
        this.makePackagePrivate(classNode);
    }

    private void makePackagePrivate(final ClassNode classNode) {
        if (classNode.nestHostClass == null && classNode.nestMembers == null) return;
        for (final MethodNode methodNode : classNode.methods) {
            methodNode.access &= ~Opcodes.ACC_PRIVATE;
        }
        for (final FieldNode fieldNode : classNode.fields) {
            fieldNode.access &= ~Opcodes.ACC_PRIVATE;
        }
        classNode.nestHostClass = null;
        classNode.nestMembers = null;
    }

}
