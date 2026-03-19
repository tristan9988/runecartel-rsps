package com.osroyale.login.impl;

import com.osroyale.Client;
import com.osroyale.Configuration;
import com.osroyale.Rasterizer2D;
import com.osroyale.Sprite;
import com.osroyale.engine.impl.MouseHandler;
import com.osroyale.login.LoginComponent;
import com.osroyale.login.ScreenType;

/**
 * Handles the message screen of login.
 *
 * @author Daniel
 */
public class MessageScreen extends LoginComponent {

    @Override
    public void render(Client client) {
        int centerX = getX();
        int centerY = getY();
        refresh(client);
        load(client, 13);

        /* Background - dark fill */
        Rasterizer2D.fillRectangle(0, 0, Client.canvasWidth, Client.canvasHeight, 0x1a0a10);

        /* Message box */
        int boxX = centerX - 180;
        int boxY = centerY - 30;
        int boxW = 360;
        int boxH = 200;
        Rasterizer2D.fillRectangle(boxX, boxY, boxW, boxH, 0x2D2D2D);
        Rasterizer2D.drawRectangle(boxX, boxY, boxW, boxH, 0xB8860B);
        Rasterizer2D.drawRectangle(boxX - 1, boxY - 1, boxW + 2, boxH + 2, 0xB8860B);

        /* Messages */
        client.boldText.drawCenteredText(0xFFD700, centerX + 5, Configuration.NAME, centerY + 10, true);
        client.regularText.drawCenteredText(0xFF6666, centerX + 5, "Error Message", centerY + 30, true);
        client.boldText.drawCenteredText(0xFFFFFF, centerX + 5, client.loginMessage1, centerY + 70, true);
        client.boldText.drawCenteredText(0xFFFFFF, centerX + 5, client.loginMessage2, centerY + 85, true);

        client.boldText.drawCenteredText(0xFFD700, centerX + 5, "[ Click anywhere to return ]", centerY + 150, true);


    }

    @Override
    public void click(Client client) {
        int centerX = getX();
        int centerY = getY();

        if (MouseHandler.clickMode3 == 1 && client.mouseInRegion(centerX - 381, centerY - 249, centerX + 381, centerY + 245)) {
            client.loginMessage1 = "";
            client.loginMessage2 = "";
            client.loginRenderer.setScreen(new MainScreen());
        }
    }

    @Override
    public ScreenType type() {
        return ScreenType.MESSAGE;
    }
}
