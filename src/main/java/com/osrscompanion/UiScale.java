package com.osrscompanion;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;

/**
 * DPI-aware scaling utility for the GUI.
 *
 * Detects the OS display scaling factor and screen resolution, then provides
 * helper methods so every font size, component dimension, and border inset
 * scales correctly on HiDPI and high-resolution displays.
 *
 * On high-resolution monitors without OS scaling (e.g. 2560x1440 at 100%),
 * the auto mode applies a heuristic scale based on horizontal resolution
 * so the UI remains readable.
 */
public final class UiScale
{
	/**
	 * The active display scale factor (1.0 = 96 DPI / 100% / 1920px baseline).
	 * Set via {@link #init(double)} before any UI is constructed.
	 */
	public static float SCALE = 1.0f;

	/**
	 * Initialise the scale factor.
	 *
	 * @param configScale the user's config value:
	 *   0  = auto-detect (uses OS DPI, falling back to resolution heuristic)
	 *   >0 = explicit multiplier (e.g. 1.25, 1.5, 2.0)
	 */
	public static void init(double configScale)
	{
		if (configScale > 0)
		{
			// Explicit user override
			SCALE = (float) configScale;
		}
		else
		{
			// Auto-detect
			SCALE = detectScale();
		}

		// Clamp to reasonable range
		if (SCALE < 1.0f) SCALE = 1.0f;
		if (SCALE > 4.0f) SCALE = 4.0f;
	}

	private static float detectScale()
	{
		float osScale = 1.0f;

		// 1) Try AffineTransform (accurate on modern Windows/macOS with DPI awareness)
		try
		{
			GraphicsDevice gd = GraphicsEnvironment
				.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			AffineTransform tx = gc.getDefaultTransform();
			osScale = (float) tx.getScaleX();
		}
		catch (Throwable ignored) {}

		// 2) Cross-check with Toolkit DPI (96 = 100%)
		if (osScale <= 1.0f)
		{
			try
			{
				int dpi = Toolkit.getDefaultToolkit().getScreenResolution();
				float tkScale = dpi / 96.0f;
				if (tkScale > osScale)
				{
					osScale = tkScale;
				}
			}
			catch (Throwable ignored) {}
		}

		// 3) If OS reports 1.0 (no scaling), apply a resolution-based heuristic
		//    so high-res monitors (2560+) don't render the UI too small.
		if (osScale <= 1.01f)
		{
			try
			{
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				int w = screenSize.width;

				// Scale relative to 1920px baseline:
				//   1920 → 1.0   (Full HD — no boost)
				//   2560 → 1.25  (QHD / 1440p)
				//   3440 → 1.5   (ultrawide QHD)
				//   3840 → 1.65  (4K)
				if (w > 1920)
				{
					float resScale = w / 1920.0f;
					// Dampen slightly so we don't over-scale
					resScale = 1.0f + (resScale - 1.0f) * 0.75f;
					if (resScale > osScale)
					{
						osScale = resScale;
					}
				}
			}
			catch (Throwable ignored) {}
		}

		return osScale;
	}

	/** Scale a pixel value. */
	public static int px(int base)
	{
		return Math.round(base * SCALE);
	}

	/** Scale a font size. */
	public static float fontSize(float base)
	{
		return base * SCALE;
	}

	/** Scale a dimension. */
	public static Dimension dim(int w, int h)
	{
		return new Dimension(Math.round(w * SCALE), Math.round(h * SCALE));
	}

	private UiScale() {}
}
