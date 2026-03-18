package com.example.zenboserver;

import android.util.Log;

import com.asus.robotframework.API.MotionControl;
import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCommand;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.WheelLights;

import fi.iki.elonen.NanoHTTPD;

import java.util.List;
import java.util.Map;

/**
 * API 端點：
 *   GET /ping                                → 測試連線
 *   GET /speak?text=你好                      → 說話（HAPPY 表情）
 *   GET /expression?face=happy               → 表情（happy/confident/worried/default）
 *   GET /spin                                → 轉一圈（~4 秒）
 *   GET /stop                                → 停止所有動作
 *   GET /follow?enable=true                  → 開啟/關閉 Follow Me
 *   GET /light?color=red&mode=solid          → 輪燈（color: red/green/blue/yellow/white/purple/cyan/orange/off）
 *                                               mode: solid/blink/breathe/marquee/off
 */
public class ZenboHttpServer extends NanoHTTPD {

    private static final String TAG = "ZenboHttpServer";
    public static final int PORT = 8080;

    private final RobotAPI robotAPI;
    private boolean isFollowing = false;
    private boolean isSpinning = false;

    public ZenboHttpServer(RobotAPI robotAPI) {
        super(PORT);
        this.robotAPI = robotAPI;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, List<String>> params = session.getParameters();
        Log.d(TAG, "Request: " + uri + " params=" + params);

        try {
            switch (uri) {

                case "/ping":
                    return ok("pong");

                case "/speak": {
                    String text = getParam(params, "text", "Hello");
                    robotAPI.robot.setExpression(RobotFace.HAPPY, text);
                    return ok("speak: " + text);
                }

                case "/expression": {
                    RobotFace rf = parseFace(getParam(params, "face", "happy"));
                    robotAPI.robot.setExpression(rf, null);
                    return ok("expression: " + rf.name());
                }

                case "/spin": {
                    if (isSpinning) return ok("spin: already spinning");
                    isSpinning = true;
                    robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.TURN_LEFT);
                    return ok("spin: started");
                }

                case "/stop": {
                    robotAPI.motion.remoteControlBody(MotionControl.Direction.Body.STOP);
                    robotAPI.motion.stopMoving();
                    isFollowing = false;
                    isSpinning  = false;
                    return ok("stop");
                }

                case "/follow": {
                    boolean enable = "true".equalsIgnoreCase(getParam(params, "enable", "true"));
                    return handleFollow(enable);
                }

                case "/light": {
                    String color = getParam(params, "color", "white");
                    String mode  = getParam(params, "mode", "solid");
                    handleLight(color, mode);
                    return ok("light: " + color + " " + mode);
                }

                default:
                    return newFixedLengthResponse(
                            Response.Status.NOT_FOUND, "text/plain", "Unknown: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling " + uri, e);
            return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
        }
    }

    // ── Follow Me ─────────────────────────────────────────────────────────────

    private Response handleFollow(boolean enable) {
        if (enable && !isFollowing) {
            robotAPI.utility.followUser();
            isFollowing = true;
            return ok("follow: started");
        } else if (!enable && isFollowing) {
            robotAPI.cancelCommand(RobotCommand.FOLLOW_USER);
            isFollowing = false;
            return ok("follow: stopped");
        }
        return ok("follow: already " + (isFollowing ? "on" : "off"));
    }

    // ── Wheel Lights ──────────────────────────────────────────────────────────

    private void handleLight(String colorName, String mode) {
        WheelLights.Lights lights = WheelLights.Lights.SYNC_BOTH;
        int color = parseColor(colorName);

        switch (mode) {
            case "solid":
                // active = duration(ms), Integer.MAX_VALUE ≈ permanent
                robotAPI.wheelLights.setColor(lights, Integer.MAX_VALUE, color);
                break;
            case "blink":
                robotAPI.wheelLights.setColor(lights, Integer.MAX_VALUE, color);
                robotAPI.wheelLights.startBlinking(lights, 1, 400, 400, 0);
                break;
            case "breathe":
                robotAPI.wheelLights.setColor(lights, Integer.MAX_VALUE, color);
                robotAPI.wheelLights.startBreathing(lights, 1, 2000, 100, 0);
                break;
            case "marquee":
                // marquee: t1=segment_time, t2=gap, number=loops(0=infinite)
                // color comes from prior setColor; don't pass colorValue into t1
                robotAPI.wheelLights.setColor(lights, Integer.MAX_VALUE, color);
                robotAPI.wheelLights.startMarquee(lights,
                        WheelLights.Direction.DIRECTION_FORWARD, 150, 50, 0);
                break;
            case "off":
                robotAPI.wheelLights.turnOff(lights, 0);
                break;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private RobotFace parseFace(String name) {
        switch (name.toLowerCase()) {
            case "confident": return RobotFace.CONFIDENT;
            case "worried":   return RobotFace.WORRIED;
            case "default":   return RobotFace.DEFAULT;
            default:          return RobotFace.HAPPY;
        }
    }

    private int parseColor(String name) {
        switch (name.toLowerCase()) {
            case "red":    return 0xFF0000;
            case "green":  return 0x00FF00;
            case "blue":   return 0x0000FF;
            case "yellow": return 0xFFFF00;
            case "purple": return 0x9C27B0;
            case "cyan":   return 0x00FFFF;
            case "orange": return 0xFF6600;
            case "off":    return 0x000000;
            default:       return 0xFFFFFF; // white
        }
    }

    private String getParam(Map<String, List<String>> params, String key, String def) {
        List<String> v = params.get(key);
        return (v != null && !v.isEmpty()) ? v.get(0) : def;
    }

    private Response ok(String msg) {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", msg);
    }
}
