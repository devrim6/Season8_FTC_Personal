package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.DisplacementTrajectory;
import com.acmerobotics.roadrunner.HolonomicController;
import com.acmerobotics.roadrunner.MecanumKinematics;
import com.acmerobotics.roadrunner.ParallelAction;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Pose2dDual;
import com.acmerobotics.roadrunner.PoseVelocity2d;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.Time;
import com.acmerobotics.roadrunner.Trajectory;
import com.acmerobotics.roadrunner.Twist2d;
import com.acmerobotics.roadrunner.Twist2dDual;
import com.acmerobotics.roadrunner.Vector2d;
import com.acmerobotics.roadrunner.VelConstraint;
import com.acmerobotics.roadrunner.ftc.Actions;
import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.gamepad.ButtonReader;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.gamepad.TriggerReader;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.checkerframework.checker.units.qual.Angle;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@TeleOp (name = "TeleOpDrive")
public class TeleOpDrive extends LinearOpMode {
    /* Init whatever you need */
    HardwareMapping robot = new HardwareMapping();
    HardwareMapping.Intake intake = robot.new Intake();
    HardwareMapping.Outtake outtake = robot.new Outtake();
    enum mode {
        TELEOP,
        HEADING_LOCK
    }
    mode currentMode = mode.TELEOP;

    /**
     *  DRIVER 1
     *   X         - Power on/off INTAKE
     *   Y         - Engage hooks on/off
     *   B         - Hanging
     *   A         - Rotate outtake 90 degrees
     *   Left/Right stick - Base controls
     *   DPAD left     - Lift ground
     *   DPAD down     - Lift 1st level
     *   DPAD right    - Lift 2nd level
     *   DPAD up       - Lift 3rd level
     *   LEFT/RIGHT BUMPER - Change intake angle
     *
     *   DRIVER 2
     *   X         - Power on/off INTAKE
     *   Y         - Engage hooks on/off
     *   B         - Launch plane
     *   A         - Rotate outtake 90 degrees
     *   Left stick Y - manual slide control
     *   Left stick X - manual outtake pitch (keep 60 degree angle (in progress))
     *   DPAD left     - Lift ground
     *   DPAD down     - Lift 1st level
     *   DPAD right    - Lift 2nd level
     *   DPAD up       - Lift 3rd level
     *   LEFT/RIGHT BUMPER - Change intake angle
     *   BACK          - Change movement mode
     *   START         - Change HEADING_LOCK target 0/180
     *
     *       _=====_                               _=====_
     *      / _____ \                             / _____ \
     *    +.-'_____'-.---------------------------.-'_____'-.+
     *   /   |     |  '.                       .'  |  _  |   \
     *  / ___| /|\ |___ \    BACK     START   / ___| (Y) |___ \
     * / |      |      | ;                   ; |              | ;
     * | | <---   ---> | |                   | |(X)       (B) | |
     * | |___   |   ___| ;  MODE             ; |___        ___| ;
     * |\    | \|/ |    /  _              _   \    | (A) |    /|
     * | \   |_____|  .','" "',        ,'" "', '.  |_____|  .' |
     * |  '-.______.-' /       \      /       \  '-._____.-'   |
     * |               |       |------|       |                |
     * |              /\       /      \       /\               |
     * |             /  '.___.'        '.___.'  \              |
     * |            /                            \             |
     *  \          /                              \           /
     *   \________/                                \_________/
     */
    // Declare a PIDF Controller to regulate heading
    private final PIDFController HEADING_PIDF = new PIDFController(1,0,0,0); //todo: tune values when you have an actual bot
                                                                                           //standard way
    @Override
    public void runOpMode() throws InterruptedException {
        //TODO: the to do list
        //add photonFTC (done)
        //masurare amperaj motoare glisiera pt determinare gear ratio optim la viteza, afaik max 4a, uitate pe spec sheet la rev
        //recalibrare pozitie cu camera april tags la backboard, triunghiulare maybe? look into it, experimenteaza cate tag-uri se vad intrun frame
        //daca localizarea ii accurate la sfarsit (cat de cat) incearca sa faci o traiectorie pt locul de lansat avion daca le trebuie la driveri, si pt hanging daca nu
        //look into angular velocity tuning pt rotiri mai rapide si consistente
        //dupa ce ii gata o autonomie, incearca sa bagi actiunile intrun alt fisier pt easier use
        //led-uri pe cuva/text pe consola sa zica ce tip de pixel ii in care parte a robotului, gen culoare sau daca exista (done)
        //implementare senzori pt pixeli in cuva, daca sunt 2 in cuva driver 1/2 numai reverse poate da la intake motors/roller (done)
        //centrare pe april tag la backboard in teleop, depinde daca ne ajuta sau nu
        //detectare de nr de pixeli pe stack, ajustare intake level in functie de. dat switch intro o camera frontala si una in spate
        //al trilea senzor/da reverse la intake in caz de 3rd pixel, bream break 100% (adafruit amazon.de)
        //fa o diagrama sa areti cum merge teleop si autonomie
        robot.init(hardwareMap);
        robot.gamepadInit(gamepad1, gamepad2);
        MecanumDrive drive = new MecanumDrive(hardwareMap, PoseTransfer.currentPose);
        // Init motors/servos/etc
        Actions.runBlocking(outtake.bottomHook("closed"), outtake.upperHook("closed")); //todo: test if you have pixels at the end of auto,
                                                                                                   // adjust this accordingly
        Actions.runBlocking(outtake.yaw(0));
        Actions.runBlocking(robot.setLedColour("upper", PoseTransfer.upperLedState),
                            robot.setLedColour("bottom", PoseTransfer.bottomLedState));

        // Variables
        double triggerSlowdown = gamepad2.right_trigger, headingTarget=180;      //TODO: transfer hook state between auto in case auto fails OR default state = closed
        boolean isIntakePowered = false, intakeManualControl = false, areHooksEngaged=true, isTeleOP=true, isOuttakeRotated=false, isHangingUp=false;
        int intakeLevel = 1;
        long startTime = System.currentTimeMillis();
        HardwareMapping.ledState bottomSensorState = HardwareMapping.ledState.OFF, upperSensorState = HardwareMapping.ledState.OFF;
        //TelemetryPacket packet = new TelemetryPacket();
        //Canvas fieldOverlay = packet.fieldOverlay();

        // Set bulk reads to AUTO, enable PhotonFTC in build.grade (TeamCode)      TODO: test difference between no photon, auto, off for engineering portfolio
        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
        telemetry.setMsTransmissionInterval(50);


        //Funky time/sugiuc
        waitForStart();
        while(opModeIsActive() && !isStopRequested()){
            Pose2d currentPose = drive.pose;                            // Memory management, don't call drive pose too much
            switch(currentMode){
                case TELEOP:
                    PoseVelocity2d currentVelPose = new PoseVelocity2d( // Slowdown by pressing right trigger, is gradual
                            new Vector2d(
                                    -gamepad2.left_stick_y/(1+triggerSlowdown),
                                    -gamepad2.left_stick_x/(1+triggerSlowdown)),
                            -gamepad2.right_stick_x/(1+triggerSlowdown*3)
                    );
                    drive.setDrivePowers(currentVelPose);

                    break;
                case HEADING_LOCK:
                    double outputVel = HEADING_PIDF.calculate(currentPose.heading.log());
                    drive.setDrivePowers(new PoseVelocity2d(
                            new Vector2d(-gamepad2.left_stick_y/(1+triggerSlowdown),
                                    -gamepad2.left_stick_x/(1+triggerSlowdown)),
                            outputVel
                    ));
                    break;
            }

            drive.updatePoseEstimate();


            // Gamepad controls

            //Slide controls
            //Driver 1 and 2
            if(gamepad2.dpad_left || gamepad1.dpad_left) Actions.runBlocking(new SequentialAction(
                    outtake.yaw(0),
                    outtake.latch("closed"),
                    outtake.runToPosition(HardwareMapping.liftHeight.GROUND)
            ));
            if(gamepad2.dpad_up || gamepad1.dpad_up) Actions.runBlocking(new SequentialAction(
                    outtake.runToPosition(HardwareMapping.liftHeight.HIGH),
                    new ParallelAction(
                            outtake.pivot(0.4, -0.4),
                            outtake.yaw(90),
                            outtake.latch("open")
                    )
            ));
            if(gamepad2.dpad_down || gamepad1.dpad_down) Actions.runBlocking(new SequentialAction(
                    outtake.runToPosition(HardwareMapping.liftHeight.LOW),
                    new ParallelAction(
                            outtake.pivot(0.4, -0.4),
                            outtake.yaw(90),
                            outtake.latch("open")
                    )
            ));
            if(gamepad2.dpad_right || gamepad1.dpad_right) Actions.runBlocking(new SequentialAction(
                    outtake.runToPosition(HardwareMapping.liftHeight.MIDDLE),
                    new ParallelAction(
                            outtake.pivot(0.4, -0.4),
                            outtake.yaw(90),
                            outtake.latch("open")
                    )
            ));

            //Manual driver 2 slide control, very VERY sketchy, virtual limits are most likely wrong, uses gradual acceleration of slides with joystick
            //todo: needs testing
            float gp1LeftStickY = gamepad1.left_stick_y;
            float gp1LeftStickX = gamepad1.left_stick_x;
            if(gp1LeftStickY>0 && robot.slideMotorRight.getCurrentPosition()<= robot.TICKS_PER_CM_Z*25
                    && robot.slideMotorLeft.getCurrentPosition()<=-robot.TICKS_PER_CM_Z*25){
                robot.slideMotorRight.setTargetPosition(robot.slideMotorRight.getCurrentPosition() + (int)(40 * gp1LeftStickY));
                robot.slideMotorLeft.setTargetPosition(robot.slideMotorLeft.getCurrentPosition() + (int)(40 * gp1LeftStickY));
                robot.slideMotorRight.setPower(0.7);
                robot.slideMotorLeft.setPower(0.7);
            } else if(gp1LeftStickY<0 && robot.slideMotorRight.getCurrentPosition()<= 10
                    && robot.slideMotorLeft.getCurrentPosition()<= -10){
                robot.slideMotorRight.setTargetPosition(robot.slideMotorRight.getCurrentPosition() - (int)(40 * gp1LeftStickY));
                robot.slideMotorLeft.setTargetPosition(robot.slideMotorLeft.getCurrentPosition() - (int)(40 * gp1LeftStickY));
                robot.slideMotorRight.setPower(0.7);
                robot.slideMotorLeft.setPower(0.7);
            }
            if(gp1LeftStickX > 0){                                          //todo: maybe works, most likely broken, needs tons of testing
                Actions.runBlocking(outtake.pivotFixedRoll(gp1LeftStickX*5));
            } else if(gp1LeftStickY < 0){
                Actions.runBlocking(outtake.pivotFixedRoll(-gp1LeftStickX*5));
            }

            //Switch between movement modes
            if(robot.gamepad1Ex.wasJustPressed(GamepadKeys.Button.BACK)){
                isTeleOP=!isTeleOP;
                if(isTeleOP) currentMode=mode.TELEOP;
                else {
                    HEADING_PIDF.reset();
                    currentMode=mode.HEADING_LOCK;
                }
            }
            // Switch between heading targets
            if(robot.gamepad1Ex.wasJustPressed(GamepadKeys.Button.START) && currentMode==mode.HEADING_LOCK){
                headingTarget+=180;
                if(headingTarget>180) headingTarget=0;
                HEADING_PIDF.setSetPoint(Math.toRadians(headingTarget));
            }

            //Hook engage control
            // If button is pressed, engage hooks and update LEDs to OFF or the colour of the locked pixel
            if(robot.gamepad1Ex.wasJustPressed(GamepadKeys.Button.Y) || robot.gamepad2Ex.wasJustPressed(GamepadKeys.Button.Y)) {
                areHooksEngaged=!areHooksEngaged;
                if(areHooksEngaged) {
                    upperSensorState = robot.checkColorRange("upper");      // Update variables and use them below
                    bottomSensorState = robot.checkColorRange("bottom");
                    Actions.runBlocking(new ParallelAction(
                            outtake.bottomHook("closed"), outtake.upperHook("closed"),
                            robot.setLedColour("upper", upperSensorState), robot.setLedColour("bottom", bottomSensorState),
                            intake.sensingOff()
                    ));
                }
                else Actions.runBlocking(new ParallelAction(
                        outtake.bottomHook("open"), outtake.upperHook("open"),
                        robot.setLedColour("upper", HardwareMapping.ledState.OFF), robot.setLedColour("bottom", HardwareMapping.ledState.OFF),
                        intake.sensingOn()
                ));
            }

            //Outtake 90 degree rotation
            if(robot.gamepad2Ex.wasJustPressed(GamepadKeys.Button.A) || robot.gamepad1Ex.wasJustPressed(GamepadKeys.Button.A)){
                isOuttakeRotated=!isOuttakeRotated;
                if(isOuttakeRotated) Actions.runBlocking(new ParallelAction(
                        outtake.yaw(90), outtake.latch("open")
                ));
                else Actions.runBlocking(new ParallelAction(
                        outtake.yaw(0), outtake.latch("closed")
                ));
            }

            //Intake power controls
            if(robot.gamepad1Ex.stateJustChanged(GamepadKeys.Button.X) || robot.gamepad2Ex.stateJustChanged(GamepadKeys.Button.X)){
                isIntakePowered=!isIntakePowered;
                if(isIntakePowered){
                    Actions.runBlocking(new ParallelAction(
                            intake.powerOn(),
                            intake.sensingOn()
                    ));
                    intakeManualControl = true;
                } else Actions.runBlocking(new ParallelAction(
                        intake.stop(),
                        intake.sensingOff()
                ));
            }

            //Intake level adjustment
            if(robot.gamepad1Ex.wasJustPressed(GamepadKeys.Button.RIGHT_BUMPER) || robot.gamepad2Ex.wasJustPressed(GamepadKeys.Button.RIGHT_BUMPER)){
                intakeLevel++; if(intakeLevel>5) intakeLevel=5;
                else Actions.runBlocking(intake.angle(intakeLevel));
            }
            if(robot.gamepad1Ex.wasJustPressed(GamepadKeys.Button.LEFT_BUMPER) || robot.gamepad2Ex.wasJustPressed(GamepadKeys.Button.LEFT_BUMPER)){
                intakeLevel--; if(intakeLevel<1) intakeLevel=1;
                else Actions.runBlocking(intake.angle(intakeLevel));
            }

            //Plane and hanging, only works if 50s have passed since teleop started, might be a pain to troubleshoot!!!!!
            if(robot.gamepad1Ex.wasJustPressed(GamepadKeys.Button.B)){
                if(System.currentTimeMillis() > startTime + 50000) Actions.runBlocking(robot.launchPlane());
            }
            if(robot.gamepad2Ex.wasJustPressed(GamepadKeys.Button.B)){
                if(System.currentTimeMillis() > startTime + 50000){
                    isHangingUp=!isHangingUp;
                    if(isHangingUp) Actions.runBlocking(robot.hangingEngage("up"));
                    else Actions.runBlocking(robot.hangingEngage("hang"));
                }
            }

            robot.gamepad1Ex.readButtons();                 // Bulk reads the gamepad
            robot.gamepad2Ex.readButtons();                 // saves on loop times

            telemetry.addData("x", currentPose.position.x);
            telemetry.addData("y", currentPose.position.y);
            telemetry.addData("heading", currentPose.heading);
            telemetry.addData("Heading target: ", headingTarget);
            telemetry.addData("Pixel upper: ", upperSensorState.toString(), "\nPixel bottom: ", bottomSensorState.toString());
            telemetry.addLine("\n---DEBUG---\n");
            telemetry.addData("slideMotorLeft amperage:", robot.slideMotorLeft.getCurrent(CurrentUnit.AMPS));
            telemetry.addData("slideMotorRight amperage:", robot.slideMotorRight.getCurrent(CurrentUnit.AMPS));
            telemetry.addData("intakeManualControl: ", intakeManualControl);
            telemetry.addData("isIntakePowered: ", isIntakePowered);
            telemetry.addData("areHooksEngaged: ", areHooksEngaged);
            telemetry.addData("isOuttakeRotated: ", isOuttakeRotated);
            telemetry.addData("isHangingUp", isHangingUp);
            telemetry.addData("isTeleOP: ", isTeleOP);
            telemetry.update();
        }
    }
}
