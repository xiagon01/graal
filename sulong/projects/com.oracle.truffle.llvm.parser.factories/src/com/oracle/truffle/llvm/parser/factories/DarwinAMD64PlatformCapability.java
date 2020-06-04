/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.parser.factories;

import com.oracle.truffle.llvm.runtime.memory.LLVMSyscallOperationNode;
import com.oracle.truffle.llvm.runtime.nodes.asm.syscall.LLVMAMD64SyscallGetPpidNode;
import com.oracle.truffle.llvm.runtime.nodes.asm.syscall.LLVMAMD64SyscallGetpidNode;
import com.oracle.truffle.llvm.runtime.nodes.asm.syscall.LLVMAMD64SyscallGettidNode;
import com.oracle.truffle.llvm.runtime.nodes.asm.syscall.LLVMAMD64SyscallMmapNodeGen;
import com.oracle.truffle.llvm.runtime.nodes.asm.syscall.LLVMSyscallExitNode;
import com.oracle.truffle.llvm.runtime.nodes.asm.syscall.LLVMUnknownSyscallNode;
import com.oracle.truffle.llvm.runtime.nodes.asm.syscall.darwin.amd64.DarwinAMD64Syscall;

final class DarwinAMD64PlatformCapability extends BasicPlatformCapability<DarwinAMD64Syscall> {

    DarwinAMD64PlatformCapability(boolean loadCxxLibraries) {
        super(DarwinAMD64Syscall.class, loadCxxLibraries);
    }

    @Override
    protected LLVMSyscallOperationNode createSyscallNode(DarwinAMD64Syscall syscall) {
        switch (syscall) {
            case SYS_mmap:
                return LLVMAMD64SyscallMmapNodeGen.create();
            case SYS_getpid:
                return new LLVMAMD64SyscallGetpidNode();
            case SYS_exit:
                return new LLVMSyscallExitNode();
            case SYS_getppid:
                return new LLVMAMD64SyscallGetPpidNode();
            case SYS_gettid:
                return new LLVMAMD64SyscallGettidNode();
            default:
                return new LLVMUnknownSyscallNode(syscall);
        }
    }
}
