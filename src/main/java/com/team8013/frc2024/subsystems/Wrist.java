package com.team8013.frc2024.subsystems;


import com.team8013.frc2024.Constants;
import com.team8013.frc2024.Ports;
import com.team8013.frc2024.loops.ILooper;
import com.team8013.frc2024.loops.Loop;
import com.team8013.lib.Conversions;
import com.team8013.lib.Util;
import com.team8013.lib.logger.Log;


import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.MotionMagicTorqueCurrentFOC;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.team254.lib.geometry.Rotation2d;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Wrist extends Subsystem{

    private static Wrist mInstance;
    private TalonFX mMotor;
    private CANcoder mCANcoder;

    private mPeriodicIO mPeriodicIO = new mPeriodicIO();

    public static Wrist getInstance() {
        if (mInstance == null) {
            mInstance = new Wrist();
        }
        return mInstance;
    }

    private Wrist() {
        mMotor = new TalonFX(Ports.WRIST,Ports.CANBUS);
        mCANcoder = new CANcoder(Ports.WRIST_CANCODER, Ports.CANBUS);

        //Customize these configs from constants in the future
        mMotor.getConfigurator().apply(new TalonFXConfiguration());
        mCANcoder.getConfigurator().apply(new CANcoderConfiguration());

        setWantNeutralBrake(false);
        resetToAbsolute();
    }

    private void resetToAbsolute(){
        double angle = Util.placeInAppropriate0To360Scope(mPeriodicIO.position_degrees, getCanCoder().getDegrees() - Constants.WristConstants.CANCODER_OFFSET);
        double absolutePosition = Conversions.degreesToRotation(angle, Constants.WristConstants.kGearRatio);
        mMotor.setPosition(absolutePosition);
    }


    private void setWantNeutralBrake(boolean brake) {
        NeutralModeValue mode = brake ? NeutralModeValue.Brake : NeutralModeValue.Coast;
        mMotor.setNeutralMode(mode);
    }


    @Override
    public void registerEnabledLoops(ILooper mEnabledLooper) {
        mEnabledLooper.register(new Loop() {
            @Override
            public void onStart(double timestamp) {
                //setSetpointMotionMagic(mConstants.kHomePosition);
            }

            @Override
            public void onLoop(double timestamp) {

            }

            @Override
            public void onStop(double timestamp) {
                setWantNeutralBrake(true);
            }
        });
    }


    @Override
    public synchronized void writePeriodicOutputs() {
        if (mPeriodicIO.mControlModeState == ControlModeState.MOTION_MAGIC){
            mMotor.setControl(new MotionMagicTorqueCurrentFOC(mPeriodicIO.demand));
        }
        else if (mPeriodicIO.mControlModeState == ControlModeState.OPEN_LOOP){
            if (mPeriodicIO.demand>1||mPeriodicIO.demand<-1){
                mMotor.setControl(new VoltageOut(mPeriodicIO.demand)); //Enable FOC in the future?
            }
            else{
                mMotor.setControl(new DutyCycleOut(mPeriodicIO.demand));
            }
        }


    }

    // public Request WristRequest(double angle, boolean waitForPosition) {
    //     return new Request() {
    //         @Override
    //         public void act() {
    //             setSetpointMotionMagic(angle);
    //             is_climb = false;
    //             is_scraping = false;
    //             updateCurrentLimits();
    //         }

    //         @Override
    //         public boolean isFinished() {
    //             return waitForPosition ? Util.epsilonEquals(mPeriodicIO.position, angle, 3.0) : true;
    //         }
    //     };
    // }


    // public Request WristWaitRequest(double angle) {
    //     return new Request() {
    //         @Override 
    //         public void act() {

    //         }

    //         @Override 
    //         public boolean isFinished() {
    //             return Util.epsilonEquals(mPeriodicIO.position_degrees, angle, 1.0);
    //         }
    //     };
    // }


    public void setSetpointMotionMagic(double degrees) {
                if (mPeriodicIO.mControlModeState != ControlModeState.MOTION_MAGIC) {
                    mPeriodicIO.mControlModeState = ControlModeState.MOTION_MAGIC;
            }
        double rotationDemand = Conversions.degreesToRotation(degrees,Constants.WristConstants.kGearRatio);
        mPeriodicIO.demand = rotationDemand;
    }

    public void setDemandOpenLoop(double demand) {
                if (mPeriodicIO.mControlModeState != ControlModeState.OPEN_LOOP) {
                    mPeriodicIO.mControlModeState = ControlModeState.OPEN_LOOP;
            }
        mPeriodicIO.demand = demand;
    }

    public Rotation2d getCanCoder() {
        return Rotation2d.fromDegrees(mCANcoder.getAbsolutePosition().getValue()*360);
    }

    @Log
    public double getWristAngleDeg(){
        return mPeriodicIO.position_degrees;
    }

    
    @Log
    public double getWristDemand(){
        return mPeriodicIO.demand;
    }
    
    @Log
    public double getWristVelocity(){
        return mPeriodicIO.velocity_radPerSec;
    }
    
    @Log
    public double getWristVolts(){
        return mPeriodicIO.output_voltage;
    }
    
    @Log
    public double getWristCurrent(){
        return mPeriodicIO.current;
    }
    

    @Log
    public double getTimestamp() {
        return mPeriodicIO.timestamp;
    }
    
    @Log
    public double getMainMotorBusVolts() {
        return mMotor.getSupplyVoltage().getValue();
    }


    public static class mPeriodicIO {
        // Inputs
        public double timestamp = 0.0;
        public double targetVelocity = 0.0;
        public double position_degrees = 0.0;
        public double velocity_radPerSec = 0.0;

        public double current = 0.0;
        public double output_voltage = 0.0;

        // Outputs
        public double demand = 0;
        public ControlModeState mControlModeState;
    }

    private enum ControlModeState{
        OPEN_LOOP,
        MOTION_MAGIC
    }

    @Override
    public synchronized void readPeriodicInputs() {
        mPeriodicIO.position_degrees = Conversions.rotationsToDegrees(mMotor.getRotorPosition().getValue(), Constants.WristConstants.kGearRatio);
        mPeriodicIO.current = mMotor.getTorqueCurrent().getValue();
        mPeriodicIO.output_voltage = mMotor.getMotorVoltage().getValue();
        mPeriodicIO.velocity_radPerSec = Conversions.rotationsToDegrees(mMotor.getVelocity().getValue(), Constants.WristConstants.kGearRatio)*Math.PI/180;
    }


    @Override
    public void outputTelemetry() {
        SmartDashboard.putNumber("WristAngle (degrees)", mPeriodicIO.position_degrees);
        SmartDashboard.putNumber("Wrist Motor Rotations", mPeriodicIO.position_degrees);
        SmartDashboard.putNumber("Wrist Demand", mPeriodicIO.demand);
        SmartDashboard.putNumber("Wrist Velocity rad/s", mPeriodicIO.velocity_radPerSec);
        SmartDashboard.putNumber("Wrist Demand", mPeriodicIO.demand);
        SmartDashboard.putNumber("Wrist Volts", mPeriodicIO.output_voltage);
        SmartDashboard.putNumber("Wrist Current", mPeriodicIO.current);
        //SmartDashboard.putString("Wrist Control State", mPeriodicIO.mControlModeState.toString());
    }
}