/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package fieldagent.asm.commons;

import fieldagent.asm.ModuleVisitor;
import fieldagent.asm.Opcodes;

/**
 * A {@link ModuleVisitor} adapter for type remapping.
 * 
 * @author Remi Forax
 */
public class ModuleRemapper extends ModuleVisitor {
    private final Remapper remapper;

    public ModuleRemapper(final ModuleVisitor mv, final Remapper remapper) {
        this(Opcodes.ASM6, mv, remapper);
    }

    protected ModuleRemapper(final int api, final ModuleVisitor mv,
            final Remapper remapper) {
        super(api, mv);
        this.remapper = remapper;
    }

    @Override
    public void visitRequire(String module, int access) {
        super.visitRequire(remapper.mapModuleName(module), access);
    }
    
    @Override
    public void visitExport(String packaze, String... modules) {
        String[] newTos = null;
        if (modules != null) {
            newTos = new String[modules.length];
            for(int i = 0 ; i < modules.length; i++) {
                newTos[i] = remapper.mapModuleName(modules[i]);
            }
        }
        super.visitExport(remapper.mapPackageName(packaze), newTos);
    }
    
    @Override
    public void visitUse(String service) {
        super.visitUse(remapper.mapType(service));
    }
    
    @Override
    public void visitProvide(String service, String impl) {
        super.visitProvide(remapper.mapType(service), remapper.mapType(impl));
    }
}
