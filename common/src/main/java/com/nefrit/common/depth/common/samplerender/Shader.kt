/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nefrit.common.depth.common.samplerender

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLException
import android.util.Log
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Matcher

/**
 * Represents a GPU shader, the state of its associated uniforms, and some additional draw state.
 */
class Shader constructor(
    render: SampleRender?,
    vertexShaderCode: String,
    fragmentShaderCode: String,
    defines: Map<String, String>?
) : Closeable {
    /**
     * A factor to be used in a blend function.
     *
     * @see [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFunc.xhtml)
     */
    enum class BlendFactor(  /* package-private */
        val glesEnum: Int
    ) {
        ZERO(GLES30.GL_ZERO), ONE(GLES30.GL_ONE), SRC_COLOR(GLES30.GL_SRC_COLOR), ONE_MINUS_SRC_COLOR(
            GLES30.GL_ONE_MINUS_SRC_COLOR
        ),
        DST_COLOR(GLES30.GL_DST_COLOR), ONE_MINUS_DST_COLOR(GLES30.GL_ONE_MINUS_DST_COLOR), SRC_ALPHA(
            GLES30.GL_SRC_ALPHA
        ),
        ONE_MINUS_SRC_ALPHA(GLES30.GL_ONE_MINUS_SRC_ALPHA), DST_ALPHA(GLES30.GL_DST_ALPHA), ONE_MINUS_DST_ALPHA(
            GLES30.GL_ONE_MINUS_DST_ALPHA
        ),
        CONSTANT_COLOR(GLES30.GL_CONSTANT_COLOR), ONE_MINUS_CONSTANT_COLOR(GLES30.GL_ONE_MINUS_CONSTANT_COLOR), CONSTANT_ALPHA(
            GLES30.GL_CONSTANT_ALPHA
        ),
        ONE_MINUS_CONSTANT_ALPHA(GLES30.GL_ONE_MINUS_CONSTANT_ALPHA);
    }

    private var programId: Int = 0
    private val uniforms: MutableMap<Int, Uniform> = HashMap()
    private var maxTextureUnit: Int = 0
    private val uniformLocations: MutableMap<String, Int> = HashMap()
    private val uniformNames: MutableMap<Int, String> = HashMap()
    private var depthTest: Boolean = true
    private var depthWrite: Boolean = true
    private var cullFace: Boolean = true
    private var sourceRgbBlend: BlendFactor = BlendFactor.ONE
    private var destRgbBlend: BlendFactor = BlendFactor.ZERO
    private var sourceAlphaBlend: BlendFactor = BlendFactor.ONE
    private var destAlphaBlend: BlendFactor = BlendFactor.ZERO

    /**
     * Constructs a [Shader] given the shader code.
     *
     * @param defines A map of shader precompiler symbols to be defined with the given names and
     * values
     */
    init {
        var vertexShaderId: Int = 0
        var fragmentShaderId: Int = 0
        val definesCode: String = createShaderDefinesCode(defines)
        try {
            vertexShaderId = createShader(
                GLES30.GL_VERTEX_SHADER, insertShaderDefinesCode(vertexShaderCode, definesCode)
            )
            fragmentShaderId = createShader(
                GLES30.GL_FRAGMENT_SHADER, insertShaderDefinesCode(fragmentShaderCode, definesCode)
            )
            programId = GLES30.glCreateProgram()
            GLError.maybeThrowGLException("Shader program creation failed", "glCreateProgram")
            GLES30.glAttachShader(programId, vertexShaderId)
            GLError.maybeThrowGLException("Failed to attach vertex shader", "glAttachShader")
            GLES30.glAttachShader(programId, fragmentShaderId)
            GLError.maybeThrowGLException("Failed to attach fragment shader", "glAttachShader")
            GLES30.glLinkProgram(programId)
            GLError.maybeThrowGLException("Failed to link shader program", "glLinkProgram")
            val linkStatus: IntArray = IntArray(1)
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus.get(0) == GLES30.GL_FALSE) {
                val infoLog: String = GLES30.glGetProgramInfoLog(programId)
                GLError.maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to retrieve shader program info log",
                    "glGetProgramInfoLog"
                )
                throw GLException(0, "Shader link failed: " + infoLog)
            }
        } catch (t: Throwable) {
            close()
            throw t
        } finally {
            // Shader objects can be flagged for deletion immediately after program creation.
            if (vertexShaderId != 0) {
                GLES30.glDeleteShader(vertexShaderId)
                GLError.maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to free vertex shader",
                    "glDeleteShader"
                )
            }
            if (fragmentShaderId != 0) {
                GLES30.glDeleteShader(fragmentShaderId)
                GLError.maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to free fragment shader",
                    "glDeleteShader"
                )
            }
        }
    }

    public override fun close() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
    }

    /**
     * Sets depth test state.
     *
     * @see [glEnable
    ](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glEnable.xhtml) */
    fun setDepthTest(depthTest: Boolean): Shader {
        this.depthTest = depthTest
        return this
    }

    /**
     * Sets depth write state.
     *
     * @see [glDepthMask](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glDepthMask.xhtml).
     */
    fun setDepthWrite(depthWrite: Boolean): Shader {
        this.depthWrite = depthWrite
        return this
    }

    /**
     * Sets cull face state.
     *
     * @see [glEnable
    ](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glEnable.xhtml) */
    fun setCullFace(cullFace: Boolean): Shader {
        this.cullFace = cullFace
        return this
    }

    /**
     * Sets blending function.
     *
     * @see [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glBlendFunc.xhtml)
     */
    fun setBlend(sourceBlend: BlendFactor, destBlend: BlendFactor): Shader {
        sourceRgbBlend = sourceBlend
        destRgbBlend = destBlend
        sourceAlphaBlend = sourceBlend
        destAlphaBlend = destBlend
        return this
    }

    /**
     * Sets blending functions separately for RGB and alpha channels.
     *
     * @see [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFuncSeparate.xhtml)
     */
    fun setBlend(
        sourceRgbBlend: BlendFactor,
        destRgbBlend: BlendFactor,
        sourceAlphaBlend: BlendFactor,
        destAlphaBlend: BlendFactor
    ): Shader {
        this.sourceRgbBlend = sourceRgbBlend
        this.destRgbBlend = destRgbBlend
        this.sourceAlphaBlend = sourceAlphaBlend
        this.destAlphaBlend = destAlphaBlend
        return this
    }

    /** Sets a texture uniform.  */
    fun setTexture(name: String, texture: Texture?): Shader {
        // Special handling for Textures. If replacing an existing texture uniform, reuse the texture
        // unit.
        val location: Int = getUniformLocation(name)
        val uniform: Uniform? = uniforms.get(location)
        val textureUnit: Int
        if (!(uniform is UniformTexture)) {
            textureUnit = maxTextureUnit++
        } else {
            textureUnit = uniform.textureUnit
        }
        uniforms.put(location, UniformTexture(textureUnit, texture))
        return this
    }

    /** Sets a `bool` uniform.  */
    fun setBool(name: String, v0: Boolean): Shader {
        val values: IntArray = intArrayOf(if (v0) 1 else 0)
        uniforms.put(getUniformLocation(name), UniformInt(values))
        return this
    }

    /** Sets an `int` uniform.  */
    fun setInt(name: String, v0: Int): Shader {
        val values: IntArray = intArrayOf(v0)
        uniforms.put(getUniformLocation(name), UniformInt(values))
        return this
    }

    /** Sets a `float` uniform.  */
    fun setFloat(name: String, v0: Float): Shader {
        val values: FloatArray = floatArrayOf(v0)
        uniforms.put(getUniformLocation(name), Uniform1f(values))
        return this
    }

    /** Sets a `vec2` uniform.  */
    fun setVec2(name: String, values: FloatArray): Shader {
        if (values.size != 2) {
            throw IllegalArgumentException("Value array length must be 2")
        }
        uniforms.put(getUniformLocation(name), Uniform2f(values.clone()))
        return this
    }

    /** Sets a `vec3` uniform.  */
    fun setVec3(name: String, values: FloatArray): Shader {
        if (values.size != 3) {
            throw IllegalArgumentException("Value array length must be 3")
        }
        uniforms.put(getUniformLocation(name), Uniform3f(values.clone()))
        return this
    }

    /** Sets a `vec4` uniform.  */
    fun setVec4(name: String, values: FloatArray): Shader {
        if (values.size != 4) {
            throw IllegalArgumentException("Value array length must be 4")
        }
        uniforms.put(getUniformLocation(name), Uniform4f(values.clone()))
        return this
    }

    /** Sets a `mat2` uniform.  */
    fun setMat2(name: String, values: FloatArray): Shader {
        if (values.size != 4) {
            throw IllegalArgumentException("Value array length must be 4 (2x2)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix2f(values.clone()))
        return this
    }

    /** Sets a `mat3` uniform.  */
    fun setMat3(name: String, values: FloatArray): Shader {
        if (values.size != 9) {
            throw IllegalArgumentException("Value array length must be 9 (3x3)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix3f(values.clone()))
        return this
    }

    /** Sets a `mat4` uniform.  */
    fun setMat4(name: String, values: FloatArray): Shader {
        if (values.size != 16) {
            throw IllegalArgumentException("Value array length must be 16 (4x4)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix4f(values.clone()))
        return this
    }

    /** Sets a `bool` array uniform.  */
    fun setBoolArray(name: String, values: BooleanArray): Shader {
        val intValues: IntArray = IntArray(values.size)
        for (i in values.indices) {
            intValues[i] = if (values.get(i)) 1 else 0
        }
        uniforms.put(getUniformLocation(name), UniformInt(intValues))
        return this
    }

    /** Sets an `int` array uniform.  */
    fun setIntArray(name: String, values: IntArray): Shader {
        uniforms.put(getUniformLocation(name), UniformInt(values.clone()))
        return this
    }

    /** Sets a `float` array uniform.  */
    fun setFloatArray(name: String, values: FloatArray): Shader {
        uniforms.put(getUniformLocation(name), Uniform1f(values.clone()))
        return this
    }

    /** Sets a `vec2` array uniform.  */
    fun setVec2Array(name: String, values: FloatArray): Shader {
        if (values.size % 2 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 2")
        }
        uniforms.put(getUniformLocation(name), Uniform2f(values.clone()))
        return this
    }

    /** Sets a `vec3` array uniform.  */
    fun setVec3Array(name: String, values: FloatArray): Shader {
        if (values.size % 3 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 3")
        }
        uniforms.put(getUniformLocation(name), Uniform3f(values.clone()))
        return this
    }

    /** Sets a `vec4` array uniform.  */
    fun setVec4Array(name: String, values: FloatArray): Shader {
        if (values.size % 4 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 4")
        }
        uniforms.put(getUniformLocation(name), Uniform4f(values.clone()))
        return this
    }

    /** Sets a `mat2` array uniform.  */
    fun setMat2Array(name: String, values: FloatArray): Shader {
        if (values.size % 4 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 4 (2x2)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix2f(values.clone()))
        return this
    }

    /** Sets a `mat3` array uniform.  */
    fun setMat3Array(name: String, values: FloatArray): Shader {
        if (values.size % 9 != 0) {
            throw IllegalArgumentException("Values array length must be divisible by 9 (3x3)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix3f(values.clone()))
        return this
    }

    /** Sets a `mat4` uniform.  */
    fun setMat4Array(name: String, values: FloatArray): Shader {
        if (values.size % 16 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 16 (4x4)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix4f(values.clone()))
        return this
    }

    /**
     * Activates the shader. Don't call this directly unless you are doing low level OpenGL code;
     * instead, prefer [SampleRender.draw].
     */
    fun lowLevelUse() {
        // Make active shader/set uniforms
        if (programId == 0) {
            throw IllegalStateException("Attempted to use freed shader")
        }
        GLES30.glUseProgram(programId)
        GLError.maybeThrowGLException("Failed to use shader program", "glUseProgram")
        GLES30.glBlendFuncSeparate(
            sourceRgbBlend.glesEnum,
            destRgbBlend.glesEnum,
            sourceAlphaBlend.glesEnum,
            destAlphaBlend.glesEnum
        )
        GLError.maybeThrowGLException("Failed to set blend mode", "glBlendFuncSeparate")
        GLES30.glDepthMask(depthWrite)
        GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask")
        if (depthTest) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLError.maybeThrowGLException("Failed to enable depth test", "glEnable")
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLError.maybeThrowGLException("Failed to disable depth test", "glDisable")
        }
        if (cullFace) {
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            GLError.maybeThrowGLException("Failed to enable backface culling", "glEnable")
        } else {
            GLES30.glDisable(GLES30.GL_CULL_FACE)
            GLError.maybeThrowGLException("Failed to disable backface culling", "glDisable")
        }
        try {
            // Remove all non-texture uniforms from the map after setting them, since they're stored as
            // part of the program.
            val obsoleteEntries: ArrayList<Int> = ArrayList(uniforms.size)
            for (entry: Map.Entry<Int, Uniform> in uniforms.entries) {
                try {
                    entry.value.use(entry.key)
                    if (!(entry.value is UniformTexture)) {
                        obsoleteEntries.add(entry.key)
                    }
                } catch (e: GLException) {
                    val name: String? = uniformNames.get(entry.key)
                    throw IllegalArgumentException("Error setting uniform `" + name + "'", e)
                }
            }
            uniforms.keys.removeAll(obsoleteEntries)
        } finally {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLError.maybeLogGLError(
                Log.WARN,
                TAG,
                "Failed to set active texture",
                "glActiveTexture"
            )
        }
    }

    private open interface Uniform {
        fun use(location: Int)
    }

    private class UniformTexture constructor(val textureUnit: Int, private val texture: Texture?) :
        Uniform {
        public override fun use(location: Int) {
            if (texture!!.getTextureId() == 0) {
                throw IllegalStateException("Tried to draw with freed texture")
            }
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
            GLError.maybeThrowGLException("Failed to set active texture", "glActiveTexture")
            GLES30.glBindTexture(texture.target.glesEnum, texture.getTextureId())
            GLError.maybeThrowGLException("Failed to bind texture", "glBindTexture")
            GLES30.glUniform1i(location, textureUnit)
            GLError.maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i")
        }
    }

    private class UniformInt constructor(private val values: IntArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniform1iv(location, values.size, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 1i", "glUniform1iv")
        }
    }

    private class Uniform1f constructor(private val values: FloatArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniform1fv(location, values.size, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 1f", "glUniform1fv")
        }
    }

    private class Uniform2f constructor(private val values: FloatArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniform2fv(location, values.size / 2, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 2f", "glUniform2fv")
        }
    }

    private class Uniform3f constructor(private val values: FloatArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniform3fv(location, values.size / 3, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 3f", "glUniform3fv")
        }
    }

    private class Uniform4f constructor(private val values: FloatArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniform4fv(location, values.size / 4, values, 0)
            GLError.maybeThrowGLException("Failed to set shader uniform 4f", "glUniform4fv")
        }
    }

    private class UniformMatrix2f constructor(private val values: FloatArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniformMatrix2fv(location, values.size / 4,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 2f",
                "glUniformMatrix2fv"
            )
        }
    }

    private class UniformMatrix3f constructor(private val values: FloatArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniformMatrix3fv(location, values.size / 9,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 3f",
                "glUniformMatrix3fv"
            )
        }
    }

    private class UniformMatrix4f constructor(private val values: FloatArray) : Uniform {
        public override fun use(location: Int) {
            GLES30.glUniformMatrix4fv(location, values.size / 16,  /*transpose=*/false, values, 0)
            GLError.maybeThrowGLException(
                "Failed to set shader uniform matrix 4f",
                "glUniformMatrix4fv"
            )
        }
    }

    private fun getUniformLocation(name: String): Int {
        val locationObject: Int? = uniformLocations.get(name)
        if (locationObject != null) {
            return locationObject
        }
        val location: Int = GLES30.glGetUniformLocation(programId, name)
        GLError.maybeThrowGLException("Failed to find uniform", "glGetUniformLocation")
        if (location == -1) {
            throw IllegalArgumentException("Shader uniform does not exist: " + name)
        }
        uniformLocations.put(name, Integer.valueOf(location))
        uniformNames.put(Integer.valueOf(location), name)
        return location
    }

    companion object {
        private val TAG: String = Shader::class.java.getSimpleName()

        /**
         * Creates a [Shader] from the given asset file names.
         *
         *
         * The file contents are interpreted as UTF-8 text.
         *
         * @param defines A map of shader precompiler symbols to be defined with the given names and
         * values
         */
        @Throws(IOException::class)
        fun createFromAssets(
            render: SampleRender,
            vertexShaderFileName: String?,
            fragmentShaderFileName: String?,
            defines: MutableMap<String, String>?
        ): Shader {
            val assets: AssetManager? = render.assets
            return Shader(
                render,
                inputStreamToString(assets!!.open(vertexShaderFileName)),
                inputStreamToString(assets.open(fragmentShaderFileName)),
                defines
            )
        }

        private fun createShader(type: Int, code: String): Int {
            val shaderId: Int = GLES30.glCreateShader(type)
            GLError.maybeThrowGLException("Shader creation failed", "glCreateShader")
            GLES30.glShaderSource(shaderId, code)
            GLError.maybeThrowGLException("Shader source failed", "glShaderSource")
            GLES30.glCompileShader(shaderId)
            GLError.maybeThrowGLException("Shader compilation failed", "glCompileShader")
            val compileStatus: IntArray = IntArray(1)
            GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus.get(0) == GLES30.GL_FALSE) {
                val infoLog: String = GLES30.glGetShaderInfoLog(shaderId)
                GLError.maybeLogGLError(
                    Log.WARN, TAG, "Failed to retrieve shader info log", "glGetShaderInfoLog"
                )
                GLES30.glDeleteShader(shaderId)
                GLError.maybeLogGLError(Log.WARN, TAG, "Failed to free shader", "glDeleteShader")
                throw GLException(0, "Shader compilation failed: " + infoLog)
            }
            return shaderId
        }

        private fun createShaderDefinesCode(defines: Map<String, String>?): String {
            if (defines == null) {
                return ""
            }
            val builder: StringBuilder = StringBuilder()
            for (entry: Map.Entry<String, String> in defines.entries) {
                builder.append("#define " + entry.key + " " + entry.value + "\n")
            }
            return builder.toString()
        }

        private fun insertShaderDefinesCode(sourceCode: String, definesCode: String): String {
            val result: String = sourceCode.replace(
                "(?m)^(\\s*#\\s*version\\s+.*)$".toRegex(),
                "$1\n" + Matcher.quoteReplacement(definesCode)
            )
            if ((result == sourceCode)) {
                // No #version specified, so just prepend source
                return definesCode + sourceCode
            }
            return result
        }

        @Throws(IOException::class)
        private fun inputStreamToString(stream: InputStream): String {
            val reader: InputStreamReader = InputStreamReader(stream, StandardCharsets.UTF_8.name())
            val buffer: CharArray = CharArray(1024 * 4)
            val builder: StringBuilder = StringBuilder()
            var amount: Int = 0
            while ((reader.read(buffer).also({ amount = it })) != -1) {
                builder.append(buffer, 0, amount)
            }
            reader.close()
            return builder.toString()
        }
    }
}