package com.osroyale.login.impl;

import com.osroyale.*;
import com.osroyale.engine.impl.KeyHandler;
import com.osroyale.engine.impl.MouseHandler;
import com.osroyale.login.LoginComponent;
import com.osroyale.login.ScreenType;
import com.osroyale.profile.Profile;
import com.osroyale.profile.ProfileManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.InputStream;

/**
 * Handles the main screen of login.
 *
 * @author Daniel
 */
public class MainScreen extends LoginComponent {

	private static final int EMAIL_CHARACTER_LIMIT = 28;
	private static Sprite customBackground = null;
	private static boolean backgroundLoaded = false;

	public MainScreen() {
	}

	private Sprite getBackgroundSprite(Client client) {
		// Try to load custom background from resources
		if (!backgroundLoaded) {
			backgroundLoaded = true;
			try {
				InputStream is = getClass().getResourceAsStream("/tarnish-background.png");
				if (is != null) {
					BufferedImage img = ImageIO.read(is);
					is.close();
					if (img != null) {
						// Scale to 765x503 (login screen size)
						int width = 765;
						int height = 503;
						BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
						java.awt.Graphics2D g2d = scaled.createGraphics();
						g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						g2d.drawImage(img, 0, 0, width, height, null);
						g2d.dispose();
						int[] pixels = ((DataBufferInt) scaled.getRaster().getDataBuffer()).getData();
						customBackground = new Sprite(width, height, 0, 0, pixels);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (customBackground != null) {
			return customBackground;
		}

		// Fallback to sprite cache
		return Client.spriteCache.get(57);
	}

	@Override
	public void render(Client client) {
		refresh(client);
		load(client, 10);

        /* Message Check */
		if (client.loginMessage1.length() > 0 || client.loginMessage2.length() > 0) {
			client.loginRenderer.setScreen(new MessageScreen());
		}

        /* Background */
		Sprite backgroundSprite = getBackgroundSprite(client);
		int alpha = 255;
		if (backgroundSprite != null) {
			backgroundSprite.drawARGBSprite(0, 0, alpha);
		} else {
			// Fallback: fill with dark color
			Rasterizer2D.fillRectangle(0, 0, 765, 503, 0x1a0a10);
		}

		// Always draw solid input boxes for visibility
		// Username box
		boolean usernameHover = client.mouseInRegion(150, 272, 416, 304);
		int boxBorderColor = usernameHover ? 0xFFD700 : 0xB8860B; // Gold colors
		int boxFillColor = 0x2D2D2D; // Dark gray fill
		Rasterizer2D.fillRectangle(150, 270, 266, 34, boxFillColor);
		Rasterizer2D.drawRectangle(150, 270, 266, 34, boxBorderColor);
		if (usernameHover || client.loginScreenCursorPos == 0) {
			Rasterizer2D.drawRectangle(149, 269, 268, 36, boxBorderColor);
		}

		// Password box
		boolean passwordHover = client.mouseInRegion(150, 320, 416, 354);
		boxBorderColor = passwordHover ? 0xFFD700 : 0xB8860B;
		Rasterizer2D.fillRectangle(150, 320, 266, 34, boxFillColor);
		Rasterizer2D.drawRectangle(150, 320, 266, 34, boxBorderColor);
		if (passwordHover || client.loginScreenCursorPos == 1) {
			Rasterizer2D.drawRectangle(149, 319, 268, 36, boxBorderColor);
		}
		
		// Draw labels for the input fields
		client.boldText.drawText(true, 150, 0xFFD700, "Username:", 265);
		client.boldText.drawText(true, 160, 0xFFFFFF, Utility.formatName(client.myUsername) + ((client.loginScreenCursorPos == 0) & (Client.tick % 40 < 20) ? "|" : ""), 295);
		client.boldText.drawText(true, 150, 0xFFD700, "Password:", 315);
		client.boldText.drawText(true, 160, 0xFFFFFF, StringUtils.toAsterisks(client.myPassword) + ((client.loginScreenCursorPos == 1) & (Client.tick % 40 < 20) ? "|" : ""), 345);

		// Remember Me checkbox - always draw solid
		int checkboxColor = Settings.REMEMBER_ME ? 0x00AA00 : 0x2D2D2D;
		Rasterizer2D.fillRectangle(150, 362, 20, 20, checkboxColor);
		Rasterizer2D.drawRectangle(150, 362, 20, 20, 0xB8860B);
		if (Settings.REMEMBER_ME) {
			// Draw checkmark
			client.boldText.drawText(true, 153, 0xFFFFFF, "X", 379);
		}
		client.boldText.drawText(true, 175, 0xFFFFFF, "Remember Me", 378);

		// Login button - always draw solid
		boolean loginHover = client.mouseInRegion(214, 414, 376, 461);
		int buttonFill = loginHover ? 0x4a3a1a : 0x3a2a1a;
		int buttonBorder = loginHover ? 0xFFD700 : 0xB8860B;
		Rasterizer2D.fillRectangle(210, 410, 166, 51, buttonFill);
		Rasterizer2D.drawRectangle(210, 410, 166, 51, buttonBorder);
		Rasterizer2D.drawRectangle(211, 411, 164, 49, buttonBorder);
		client.boldText.drawCenteredText(0xFFD700, 293, "LOGIN", 440, true);

		// Saved profiles section header
		client.boldText.drawText(true, 500, 0xFFD700, "Saved Profiles:", 230);

		int profileY = 245;
		for (int i = 0; i < ProfileManager.MAX_PROFILES; i++, profileY += 65) {
			Profile profile = ProfileManager.profiles.get(i);

			// Always draw profile box
			boolean isEmptySlot = (profile == null || profile.emptySlot());
			int profileBorder = isEmptySlot ? 0x666666 : 0xB8860B;
			int profileFill = 0x2D2D2D;

			Rasterizer2D.fillRectangle(484, profileY - 5, 160, 55, profileFill);
			Rasterizer2D.drawRectangle(484, profileY - 5, 160, 55, profileBorder);

			if (isEmptySlot) {
				client.boldText.drawCenteredText(0x666666, 564, "Empty Slot", profileY + 29, true);
			} else {
				// Draw X button for delete
				Rasterizer2D.fillRectangle(485, profileY - 3, 12, 12, 0xAA0000);
				client.smallFont.drawCenteredText(0xFFFFFF, 491, "X", profileY + 6, true);

				profile.drawProfileHead(499, profileY);
				client.boldText.drawCenteredText(0xFFD700, 575, Utility.formatName(profile.getUsername()), profileY + 29, true);
			}
		}
	}

	@Override
	public void click(Client client) {
        /* Username */
		if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(150, 272, 416, 304)) {
			client.loginScreenCursorPos = 0;
		}

        /* Password */
		if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(150, 320, 416, 354)) {
			client.loginScreenCursorPos = 1;
		}

        /* Remember Me */
		if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(150, 360, 172, 383)) {
			Settings.REMEMBER_ME = !Settings.REMEMBER_ME;
		}

		int profileY = 245;
		for (int i = 0; i < ProfileManager.MAX_PROFILES; i++, profileY += 65) {
			Profile profile = ProfileManager.profiles.get(i);
			if (profile == null) {
				continue;
			}

			if (profile.emptySlot()) {
				continue;
			}

			if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(483, profileY - 5, 497, profileY + 10)) {
				ProfileManager.delete(profile);
				break;
			} else if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(488, profileY, 635, profileY + 50)) {
				if (!Client.loggedIn) {
					client.myUsername = profile.getUsername();
					client.myPassword = profile.getPassword();
					client.attemptLogin(client.myUsername, client.myPassword, false);
				}
				break;
			}
		}

        /* Login Buttons */
		if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(214, 414, 376, 461)) {
			if (!Client.loggedIn) {
				client.attemptLogin(client.myUsername, client.myPassword, false);
			}
		}

        /* Writing */
		handleWriting(client);
	}

	/**
	 * Handles writing in the client.
	 */
	private void handleWriting(Client client) {
		do {
			int line = KeyHandler.instance.readChar();
			if (line == -1)
				break;
			boolean flag = false;
			for (int index = 0; index < Client.validUserPassChars.length(); index++) {
				if (line != Client.validUserPassChars.charAt(index))
					continue;
				flag = true;
				break;
			}

			// Main account username
			if (client.loginScreenCursorPos == 0) {
				if (line == 8 && client.myUsername.length() > 0)
					client.myUsername = client.myUsername.substring(0, client.myUsername.length() - 1);
				if (line == 9 || line == 10 || line == 13) {
					client.loginScreenCursorPos = 1;
				}
				if (flag) {
					client.myUsername += (char) line;
				}

				if (client.myUsername.length() > EMAIL_CHARACTER_LIMIT) {
					client.myUsername = client.myUsername.substring(0, EMAIL_CHARACTER_LIMIT);
				}

				// Main account password
			} else if (client.loginScreenCursorPos == 1) {
				if (line == 8 && client.myPassword.length() > 0)
					client.myPassword = client.myPassword.substring(0, client.myPassword.length() - 1);
				if (line == 9 || line == 10 || line == 13) {
					client.attemptLogin(client.myUsername, client.myPassword, false);
				}
				if (flag) {
					client.myPassword += (char) line;
				}
				if (client.myPassword.length() > 20) {
					client.myPassword = client.myPassword.substring(0, 20);
				}
			}
		} while (true);
	}

	@Override
	public ScreenType type() {
		return ScreenType.MAIN;
	}
}
