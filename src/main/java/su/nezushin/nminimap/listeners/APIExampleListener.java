package su.nezushin.nminimap.listeners;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.map.MapPalette;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.api.events.AsyncFrameRenderEvent;
import su.nezushin.nminimap.api.events.AsyncMapRenderEvent;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.frames.Frame;
import su.nezushin.nminimap.frames.FrameLayer;
import su.nezushin.nminimap.frames.FrameManager;
import su.nezushin.nminimap.markers.NMapMarker;
import su.nezushin.nminimap.markers.impl.LocationMarker;
import su.nezushin.nminimap.markers.impl.PositionMarker;
import su.nezushin.nminimap.util.ColorUtil;

import java.util.List;

public class APIExampleListener implements Listener {

    @EventHandler
    public void drawDot(AsyncMapRenderEvent event){
        byte[] mapData = event.getMapData();

        //middle of the map
        int x = 64;
        int y = 64;

        //Accepts only colors from ColorUtil.colors. Another values will result transparent color
        mapData[x + (y * 128)] = ColorUtil.exactColor(ColorUtil.colors[10]);


        int anotherX = 1;//first line is reserved for internal use. Displayed map has resolution 127 x 128
        int anotherY = 0;

        //Similar to deprecated MapPalette.matchColor(). Will find most nearest color.
        mapData[anotherX + (anotherY * 128)] = ColorUtil.getNearestColor(Color.fromRGB(255, 0, 0));
    }

    @EventHandler
    public void drawMarker(AsyncMarkerRenderEvent e){
        List<NMapMarker> markers = e.getMarkers();

        String markerIcon = "player";//Icon from NMinimap/markers directory

        //Add marker with fixed location
        markers.add(new LocationMarker(markerIcon, new Location(e.getPlayer().getPlayer().getWorld(), 1, 1, 1)));

        int positionMarkerX = 0;//Accepts values from -127 to 127. -127 - left. 127 - right
        int positionMarkerY = 0;//-127 - top. 127 - bottom
        int positionMarkerRotation = 0;//from 0 to 256. 0 points top, 128 points bottom

        //Add marker with relative position on map.
        markers.add(new PositionMarker(markerIcon, positionMarkerX, positionMarkerY, positionMarkerRotation));

    }

    @EventHandler
    public void drawFrame(AsyncFrameRenderEvent e) {
        //Layers of the frame the player selected. Already copied, change them as you like
        List<FrameLayer> layers = e.getLayers();

        for (FrameLayer layer : layers)
            layer.setZIndex(200);//0-127 behind the map, 128-255 in front

        FrameManager frames = NMinimap.getInstance().getFrameManager();

        //Every packed variant of an image from the NMinimap/frames directory
        List<FrameLayer> variants = frames.getLayers("inventory_square");

        //Layers from the manager are shared between players, so copy before changing
        if (!variants.isEmpty()) {
            FrameLayer layer = variants.get(0).copy();
            layer.setRotation(64);//0-255 for a full turn, 0 points up
            layers.add(layer);
        }

        //Layers of another frame can be used too
        Frame frame = frames.getFrame("inventory");
        if (frame != null)
            for (FrameLayer layer : frame.layers(e.getPlayer().isRound()))
                layers.add(layer.copy());
    }
}
