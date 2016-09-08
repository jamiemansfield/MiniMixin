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

package uk.jamierocks.minimixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import net.minecraft.launchwrapper.Launch;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public final class Mixins {

    private static final Gson GSON = new Gson();

    public static Map<String, List<CtClass>> MIXINS = Maps.newHashMap();

    public static void setupEnvironment() {
        Launch.classLoader.registerTransformer("uk.jamierocks.minimixin.transformer.MixinTransformer");
    }

    public static void addConfiguration(String... configs) {
        for (String config : configs) {
            MixinConfig mixinConfig = GSON.fromJson(new InputStreamReader(Launch.classLoader.getResourceAsStream(config)), MixinConfig.class);

            for (String mixin : mixinConfig.getMixins()) {
                try {
                    final CtClass mixinClass = ClassPool.getDefault().get(mixinConfig.getPackageName() + "." + mixin);
                    if (!mixinClass.hasAnnotation(Mixin.class)) continue;;
                    final Mixin mixinAnnot = (Mixin) mixinClass.getAnnotation(Mixin.class);

                    final List<CtClass> mixins = MIXINS.getOrDefault(mixinClass.getName(), Lists.newArrayList());
                    mixins.add(mixinClass);
                    MIXINS.put(mixinAnnot.value().getName(), mixins);
                } catch (NotFoundException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
