/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared.sdl;

public class SDLRender {

    public static final String
            SDL_SOFTWARE_RENDERER = "software",
            SDL_GPU_RENDERER      = "gpu";

    public static final int
            SDL_TEXTUREACCESS_STATIC    = 0,
            SDL_TEXTUREACCESS_STREAMING = 1,
            SDL_TEXTUREACCESS_TARGET    = 2;

    public static final int
            SDL_TEXTURE_ADDRESS_INVALID = -1,
            SDL_TEXTURE_ADDRESS_AUTO    = 0,
            SDL_TEXTURE_ADDRESS_CLAMP   = 1,
            SDL_TEXTURE_ADDRESS_WRAP    = 2;

    public static final int
            SDL_LOGICAL_PRESENTATION_DISABLED      = 0,
            SDL_LOGICAL_PRESENTATION_STRETCH       = 1,
            SDL_LOGICAL_PRESENTATION_LETTERBOX     = 2,
            SDL_LOGICAL_PRESENTATION_OVERSCAN      = 3,
            SDL_LOGICAL_PRESENTATION_INTEGER_SCALE = 4;

    public static final String
            SDL_PROP_RENDERER_CREATE_NAME_STRING                               = "SDL.renderer.create.name",
            SDL_PROP_RENDERER_CREATE_WINDOW_POINTER                            = "SDL.renderer.create.window",
            SDL_PROP_RENDERER_CREATE_SURFACE_POINTER                           = "SDL.renderer.create.surface",
            SDL_PROP_RENDERER_CREATE_OUTPUT_COLORSPACE_NUMBER                  = "SDL.renderer.create.output_colorspace",
            SDL_PROP_RENDERER_CREATE_PRESENT_VSYNC_NUMBER                      = "SDL.renderer.create.present_vsync",
            SDL_PROP_RENDERER_CREATE_GPU_DEVICE_POINTER                        = "SDL.renderer.create.gpu.device",
            SDL_PROP_RENDERER_CREATE_GPU_SHADERS_SPIRV_BOOLEAN                 = "SDL.renderer.create.gpu.shaders_spirv",
            SDL_PROP_RENDERER_CREATE_GPU_SHADERS_DXIL_BOOLEAN                  = "SDL.renderer.create.gpu.shaders_dxil",
            SDL_PROP_RENDERER_CREATE_GPU_SHADERS_MSL_BOOLEAN                   = "SDL.renderer.create.gpu.shaders_msl",
            SDL_PROP_RENDERER_CREATE_VULKAN_INSTANCE_POINTER                   = "SDL.renderer.create.vulkan.instance",
            SDL_PROP_RENDERER_CREATE_VULKAN_SURFACE_NUMBER                     = "SDL.renderer.create.vulkan.surface",
            SDL_PROP_RENDERER_CREATE_VULKAN_PHYSICAL_DEVICE_POINTER            = "SDL.renderer.create.vulkan.physical_device",
            SDL_PROP_RENDERER_CREATE_VULKAN_DEVICE_POINTER                     = "SDL.renderer.create.vulkan.device",
            SDL_PROP_RENDERER_CREATE_VULKAN_GRAPHICS_QUEUE_FAMILY_INDEX_NUMBER = "SDL.renderer.create.vulkan.graphics_queue_family_index",
            SDL_PROP_RENDERER_CREATE_VULKAN_PRESENT_QUEUE_FAMILY_INDEX_NUMBER  = "SDL.renderer.create.vulkan.present_queue_family_index";

    public static final String
            SDL_PROP_RENDERER_NAME_STRING                               = "SDL.renderer.name",
            SDL_PROP_RENDERER_WINDOW_POINTER                            = "SDL.renderer.window",
            SDL_PROP_RENDERER_SURFACE_POINTER                           = "SDL.renderer.surface",
            SDL_PROP_RENDERER_VSYNC_NUMBER                              = "SDL.renderer.vsync",
            SDL_PROP_RENDERER_MAX_TEXTURE_SIZE_NUMBER                   = "SDL.renderer.max_texture_size",
            SDL_PROP_RENDERER_TEXTURE_FORMATS_POINTER                   = "SDL.renderer.texture_formats",
            SDL_PROP_RENDERER_TEXTURE_WRAPPING_BOOLEAN                  = "SDL.renderer.texture_wrapping",
            SDL_PROP_RENDERER_OUTPUT_COLORSPACE_NUMBER                  = "SDL.renderer.output_colorspace",
            SDL_PROP_RENDERER_HDR_ENABLED_BOOLEAN                       = "SDL.renderer.HDR_enabled",
            SDL_PROP_RENDERER_SDR_WHITE_POINT_FLOAT                     = "SDL.renderer.SDR_white_point",
            SDL_PROP_RENDERER_HDR_HEADROOM_FLOAT                        = "SDL.renderer.HDR_headroom",
            SDL_PROP_RENDERER_D3D9_DEVICE_POINTER                       = "SDL.renderer.d3d9.device",
            SDL_PROP_RENDERER_D3D11_DEVICE_POINTER                      = "SDL.renderer.d3d11.device",
            SDL_PROP_RENDERER_D3D11_SWAPCHAIN_POINTER                   = "SDL.renderer.d3d11.swap_chain",
            SDL_PROP_RENDERER_D3D12_DEVICE_POINTER                      = "SDL.renderer.d3d12.device",
            SDL_PROP_RENDERER_D3D12_SWAPCHAIN_POINTER                   = "SDL.renderer.d3d12.swap_chain",
            SDL_PROP_RENDERER_D3D12_COMMAND_QUEUE_POINTER               = "SDL.renderer.d3d12.command_queue",
            SDL_PROP_RENDERER_VULKAN_INSTANCE_POINTER                   = "SDL.renderer.vulkan.instance",
            SDL_PROP_RENDERER_VULKAN_SURFACE_NUMBER                     = "SDL.renderer.vulkan.surface",
            SDL_PROP_RENDERER_VULKAN_PHYSICAL_DEVICE_POINTER            = "SDL.renderer.vulkan.physical_device",
            SDL_PROP_RENDERER_VULKAN_DEVICE_POINTER                     = "SDL.renderer.vulkan.device",
            SDL_PROP_RENDERER_VULKAN_GRAPHICS_QUEUE_FAMILY_INDEX_NUMBER = "SDL.renderer.vulkan.graphics_queue_family_index",
            SDL_PROP_RENDERER_VULKAN_PRESENT_QUEUE_FAMILY_INDEX_NUMBER  = "SDL.renderer.vulkan.present_queue_family_index",
            SDL_PROP_RENDERER_VULKAN_SWAPCHAIN_IMAGE_COUNT_NUMBER       = "SDL.renderer.vulkan.swapchain_image_count",
            SDL_PROP_RENDERER_GPU_DEVICE_POINTER                        = "SDL.renderer.gpu.device";

    public static final String
            SDL_PROP_TEXTURE_CREATE_COLORSPACE_NUMBER           = "SDL.texture.create.colorspace",
            SDL_PROP_TEXTURE_CREATE_FORMAT_NUMBER               = "SDL.texture.create.format",
            SDL_PROP_TEXTURE_CREATE_ACCESS_NUMBER               = "SDL.texture.create.access",
            SDL_PROP_TEXTURE_CREATE_WIDTH_NUMBER                = "SDL.texture.create.width",
            SDL_PROP_TEXTURE_CREATE_HEIGHT_NUMBER               = "SDL.texture.create.height",
            SDL_PROP_TEXTURE_CREATE_PALETTE_POINTER             = "SDL.texture.create.palette",
            SDL_PROP_TEXTURE_CREATE_SDR_WHITE_POINT_FLOAT       = "SDL.texture.create.SDR_white_point",
            SDL_PROP_TEXTURE_CREATE_HDR_HEADROOM_FLOAT          = "SDL.texture.create.HDR_headroom",
            SDL_PROP_TEXTURE_CREATE_D3D11_TEXTURE_POINTER       = "SDL.texture.create.d3d11.texture",
            SDL_PROP_TEXTURE_CREATE_D3D11_TEXTURE_U_POINTER     = "SDL.texture.create.d3d11.texture_u",
            SDL_PROP_TEXTURE_CREATE_D3D11_TEXTURE_V_POINTER     = "SDL.texture.create.d3d11.texture_v",
            SDL_PROP_TEXTURE_CREATE_D3D12_TEXTURE_POINTER       = "SDL.texture.create.d3d12.texture",
            SDL_PROP_TEXTURE_CREATE_D3D12_TEXTURE_U_POINTER     = "SDL.texture.create.d3d12.texture_u",
            SDL_PROP_TEXTURE_CREATE_D3D12_TEXTURE_V_POINTER     = "SDL.texture.create.d3d12.texture_v",
            SDL_PROP_TEXTURE_CREATE_METAL_PIXELBUFFER_POINTER   = "SDL.texture.create.metal.pixelbuffer",
            SDL_PROP_TEXTURE_CREATE_OPENGL_TEXTURE_NUMBER       = "SDL.texture.create.opengl.texture",
            SDL_PROP_TEXTURE_CREATE_OPENGL_TEXTURE_UV_NUMBER    = "SDL.texture.create.opengl.texture_uv",
            SDL_PROP_TEXTURE_CREATE_OPENGL_TEXTURE_U_NUMBER     = "SDL.texture.create.opengl.texture_u",
            SDL_PROP_TEXTURE_CREATE_OPENGL_TEXTURE_V_NUMBER     = "SDL.texture.create.opengl.texture_v",
            SDL_PROP_TEXTURE_CREATE_OPENGLES2_TEXTURE_NUMBER    = "SDL.texture.create.opengles2.texture",
            SDL_PROP_TEXTURE_CREATE_OPENGLES2_TEXTURE_UV_NUMBER = "SDL.texture.create.opengles2.texture_uv",
            SDL_PROP_TEXTURE_CREATE_OPENGLES2_TEXTURE_U_NUMBER  = "SDL.texture.create.opengles2.texture_u",
            SDL_PROP_TEXTURE_CREATE_OPENGLES2_TEXTURE_V_NUMBER  = "SDL.texture.create.opengles2.texture_v",
            SDL_PROP_TEXTURE_CREATE_VULKAN_TEXTURE_NUMBER       = "SDL.texture.create.vulkan.texture",
            SDL_PROP_TEXTURE_CREATE_VULKAN_LAYOUT_NUMBER        = "SDL.texture.create.vulkan.layout",
            SDL_PROP_TEXTURE_CREATE_GPU_TEXTURE_POINTER         = "SDL.texture.create.gpu.texture",
            SDL_PROP_TEXTURE_CREATE_GPU_TEXTURE_UV_POINTER      = "SDL.texture.create.gpu.texture_uv",
            SDL_PROP_TEXTURE_CREATE_GPU_TEXTURE_U_POINTER       = "SDL.texture.create.gpu.texture_u",
            SDL_PROP_TEXTURE_CREATE_GPU_TEXTURE_V_POINTER       = "SDL.texture.create.gpu.texture_v";

    public static final String
            SDL_PROP_TEXTURE_COLORSPACE_NUMBER               = "SDL.texture.colorspace",
            SDL_PROP_TEXTURE_FORMAT_NUMBER                   = "SDL.texture.format",
            SDL_PROP_TEXTURE_ACCESS_NUMBER                   = "SDL.texture.access",
            SDL_PROP_TEXTURE_WIDTH_NUMBER                    = "SDL.texture.width",
            SDL_PROP_TEXTURE_HEIGHT_NUMBER                   = "SDL.texture.height",
            SDL_PROP_TEXTURE_SDR_WHITE_POINT_FLOAT           = "SDL.texture.SDR_white_point",
            SDL_PROP_TEXTURE_HDR_HEADROOM_FLOAT              = "SDL.texture.HDR_headroom",
            SDL_PROP_TEXTURE_D3D11_TEXTURE_POINTER           = "SDL.texture.d3d11.texture",
            SDL_PROP_TEXTURE_D3D11_TEXTURE_U_POINTER         = "SDL.texture.d3d11.texture_u",
            SDL_PROP_TEXTURE_D3D11_TEXTURE_V_POINTER         = "SDL.texture.d3d11.texture_v",
            SDL_PROP_TEXTURE_D3D12_TEXTURE_POINTER           = "SDL.texture.d3d12.texture",
            SDL_PROP_TEXTURE_D3D12_TEXTURE_U_POINTER         = "SDL.texture.d3d12.texture_u",
            SDL_PROP_TEXTURE_D3D12_TEXTURE_V_POINTER         = "SDL.texture.d3d12.texture_v",
            SDL_PROP_TEXTURE_OPENGL_TEXTURE_NUMBER           = "SDL.texture.opengl.texture",
            SDL_PROP_TEXTURE_OPENGL_TEXTURE_UV_NUMBER        = "SDL.texture.opengl.texture_uv",
            SDL_PROP_TEXTURE_OPENGL_TEXTURE_U_NUMBER         = "SDL.texture.opengl.texture_u",
            SDL_PROP_TEXTURE_OPENGL_TEXTURE_V_NUMBER         = "SDL.texture.opengl.texture_v",
            SDL_PROP_TEXTURE_OPENGL_TEXTURE_TARGET_NUMBER    = "SDL.texture.opengl.target",
            SDL_PROP_TEXTURE_OPENGL_TEX_W_FLOAT              = "SDL.texture.opengl.tex_w",
            SDL_PROP_TEXTURE_OPENGL_TEX_H_FLOAT              = "SDL.texture.opengl.tex_h",
            SDL_PROP_TEXTURE_OPENGLES2_TEXTURE_NUMBER        = "SDL.texture.opengles2.texture",
            SDL_PROP_TEXTURE_OPENGLES2_TEXTURE_UV_NUMBER     = "SDL.texture.opengles2.texture_uv",
            SDL_PROP_TEXTURE_OPENGLES2_TEXTURE_U_NUMBER      = "SDL.texture.opengles2.texture_u",
            SDL_PROP_TEXTURE_OPENGLES2_TEXTURE_V_NUMBER      = "SDL.texture.opengles2.texture_v",
            SDL_PROP_TEXTURE_OPENGLES2_TEXTURE_TARGET_NUMBER = "SDL.texture.opengles2.target",
            SDL_PROP_TEXTURE_VULKAN_TEXTURE_NUMBER           = "SDL.texture.vulkan.texture",
            SDL_PROP_TEXTURE_GPU_TEXTURE_POINTER             = "SDL.texture.gpu.texture",
            SDL_PROP_TEXTURE_GPU_TEXTURE_UV_POINTER          = "SDL.texture.gpu.texture_uv",
            SDL_PROP_TEXTURE_GPU_TEXTURE_U_POINTER           = "SDL.texture.gpu.texture_u",
            SDL_PROP_TEXTURE_GPU_TEXTURE_V_POINTER           = "SDL.texture.gpu.texture_v";

    public static final int
            SDL_RENDERER_VSYNC_DISABLED = 0,
            SDL_RENDERER_VSYNC_ADAPTIVE = -1;

    public static final int SDL_DEBUG_TEXT_FONT_CHARACTER_SIZE = 8;

    protected SDLRender() {
        throw new UnsupportedOperationException();
    }
}
