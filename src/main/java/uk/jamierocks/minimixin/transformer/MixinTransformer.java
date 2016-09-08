/*
 * This file is part of MiniMixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, Jamie Mansfield <https://www.jamierocks.uk/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package uk.jamierocks.minimixin.transformer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import net.minecraft.launchwrapper.IClassTransformer;
import uk.jamierocks.minimixin.Inject;
import uk.jamierocks.minimixin.Mixins;
import uk.jamierocks.minimixin.Overwrite;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MixinTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (Mixins.MIXINS.containsKey(name)) {
            CtClass clazz;
            try {
                clazz = ClassPool.getDefault().get(name);
            } catch (NotFoundException e) {
                e.printStackTrace();
                return basicClass;
            }

            final Map<String, CtMethod> overwrites = Maps.newHashMap();
            final Map<String, CtMethod> injects = Maps.newHashMap();
            final List<CtMethod> methods = Lists.newArrayList();

            for (CtClass mixinClass : Mixins.MIXINS.get(name)) {
                for (CtMethod ctMethod : mixinClass.getMethods()) {
                    if (ctMethod.hasAnnotation(Overwrite.class)) {
                        overwrites.put(ctMethod.getName(), ctMethod);
                    } else if (ctMethod.hasAnnotation(Inject.class)) {
                        try {
                            injects.put(((Inject) ctMethod.getAnnotation(Inject.class)).method(), ctMethod);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else if (ctMethod.getDeclaringClass() == mixinClass) {
                        methods.add(ctMethod);
                    }
                }

                try {
                    for (CtClass ctInterface : mixinClass.getInterfaces()) {
                        clazz.addInterface(ctInterface);
                    }
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }

            for (CtMethod ctMethod : methods) {
                try {
                    CtMethod newMethod = new CtMethod(ctMethod, clazz, null);
                    clazz.addMethod(newMethod);
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            }

            for (CtMethod ctMethod : injects.values()) {
                try {
                    CtMethod newMethod = new CtMethod(ctMethod.getReturnType(), "inject$" + ctMethod.getName(), ctMethod.getParameterTypes(), clazz);
                    newMethod.setBody(ctMethod, null);

                    clazz.addMethod(newMethod);
                } catch (NotFoundException | CannotCompileException e) {
                    e.printStackTrace();
                }
            }

            for (CtMethod ctMethod : clazz.getMethods()) {
                CtMethod overwriteMethod = overwrites.get(ctMethod.getName());
                CtMethod injectMethod = injects.get(ctMethod.getName());

                if (overwriteMethod != null) {
                    try {
                        ctMethod.setBody(overwriteMethod, null);
                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                    }
                }
                if (injectMethod != null) {
                    try {
                        Inject inject = (Inject) injectMethod.getAnnotation(Inject.class);

                        switch (inject.at()) {
                            case HEAD:
                                ctMethod.insertBefore("this.inject$" + injectMethod.getName() + "();");
                                break;
                            case RETURN:
                                ctMethod.insertAfter("this.inject$" + injectMethod.getName() + "();");
                                break;
                        }
                    } catch (ClassNotFoundException | CannotCompileException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                return clazz.toBytecode();
            } catch (IOException | CannotCompileException e) {
                e.printStackTrace();
            }
        }

        return basicClass;
    }

}
