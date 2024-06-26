package com.team8013.frc2024.auto.modes;

import java.util.List;

import com.team8013.frc2024.Constants;
import com.team8013.frc2024.Robot;
import com.team8013.frc2024.auto.AutoModeBase;
import com.team8013.frc2024.auto.AutoModeEndedException;
import com.team8013.frc2024.auto.AutoTrajectoryReader;
import com.team8013.frc2024.auto.actions.LambdaAction;
import com.team8013.frc2024.auto.actions.ParallelAction;
import com.team8013.frc2024.auto.actions.SeriesAction;
import com.team8013.frc2024.auto.actions.SwerveTrajectoryAction;
import com.team8013.frc2024.auto.actions.WaitAction;
import com.team8013.frc2024.auto.actions.WaitToPassXCoordinateAction;
import com.team8013.frc2024.shuffleboard.ShuffleBoardInteractions;
import com.team8013.frc2024.subsystems.Drive;
import com.team8013.frc2024.subsystems.EndEffectorREV;
import com.team8013.frc2024.subsystems.Limelight;
import com.team8013.frc2024.subsystems.Superstructure;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.Trajectory;

public class StageStraightToMiddle extends AutoModeBase {
//worked on field
        private Superstructure mSuperstructure;
        // private ControlBoard mControlBoard;
        private Limelight mLimelight;
        private EndEffectorREV mEffector;

        // required PathWeaver trajectory paths
        String path_C1 = "paths/2024Paths/StageSideStraightToMiddle_A.path";
        String path_C2 = "paths/2024Paths/StageSideStraightToMiddle_B.path";
        String path_C3 = "paths/2024Paths/StageSideStraightToMiddle_C.path";

        // trajectories
        SwerveTrajectoryAction driveToThirdNote1;
        final Trajectory drivePath_C1;

        SwerveTrajectoryAction driveToThirdNote2;
        final Trajectory drivePath_C2;

        SwerveTrajectoryAction driveToThirdNote3;
        final Trajectory drivePath_C3;

        public StageStraightToMiddle() {
                mSuperstructure = Superstructure.getInstance();
                // mControlBoard = ControlBoard.getInstance();
                mLimelight = Limelight.getInstance();
                mEffector = EndEffectorREV.getInstance();

                drivePath_C1 = AutoTrajectoryReader.generateTrajectoryFromFile(path_C1,
                                Constants.AutoConstants.createConfig(5, 2.5, 0.0, 0));
                driveToThirdNote1 = new SwerveTrajectoryAction(drivePath_C1, Rotation2d.fromDegrees(240));
                ShuffleBoardInteractions.getInstance().mFieldView.addTrajectory("Traj", drivePath_C1);

                drivePath_C2 = AutoTrajectoryReader.generateTrajectoryFromFile(path_C2,
                                Constants.AutoConstants.createConfig(4.5, 3, 0, 0));
                driveToThirdNote2 = new SwerveTrajectoryAction(drivePath_C2, Rotation2d.fromDegrees(180));
                ShuffleBoardInteractions.getInstance().mFieldView.addTrajectory("Traj", drivePath_C2);

                drivePath_C3 = AutoTrajectoryReader.generateTrajectoryFromFile(path_C3,
                                Constants.AutoConstants.createConfig(3, 2, 0, 0));
                driveToThirdNote3 = new SwerveTrajectoryAction(drivePath_C3, Rotation2d.fromDegrees(180));
                ShuffleBoardInteractions.getInstance().mFieldView.addTrajectory("Traj", drivePath_C3);

        }

        @Override
        protected void routine() throws AutoModeEndedException {
                runAction(new LambdaAction(() -> Drive.getInstance().resetOdometry(getStartingPose())));
                // mLimelight.shootAgainstSubwooferSideAngle(true);

                System.out.println("Running 2 note auto");
                mSuperstructure.autoShot();
                runAction(new WaitAction(1.2));
                mSuperstructure.disableAutoShot();
                mSuperstructure.setSuperstuctureStow();

                runAction(new ParallelAction(List.of(
                                driveToThirdNote1,
                                new SeriesAction(List.of(
                                                new WaitToPassXCoordinateAction(13),
                                                new LambdaAction((() -> mSuperstructure
                                                                .setSuperstuctureIntakingGround())),
                                                new WaitAction(0.1),
                                                new LambdaAction(() -> Drive.getInstance()
                                                                .setAutoHeading(Rotation2d.fromDegrees(150))),
                                                new WaitAction(1.6),
                                                new LambdaAction(() -> Drive.getInstance()
                                                                .setAutoHeading(Rotation2d.fromDegrees(180))),
                                                new WaitAction(0.15),
                                                new LambdaAction(() -> mSuperstructure
                                                                .setSuperstuctureStow()))))));
                mSuperstructure.setSuperstuctureStow();

                if (mEffector.hasGamePiece()){
                        runAction(new ParallelAction(List.of(
                                        driveToThirdNote2,
                                        new SeriesAction(List.of(
                                                        new WaitAction(0.1),
                                                        new LambdaAction(() -> mSuperstructure
                                                                        .setSuperstuctureTransferToShooter()),
                                                        new WaitAction(0.3),
                                                        new LambdaAction(() -> Drive.getInstance()
                                                                        .setAutoHeading(Rotation2d.fromDegrees(240))), //was 227
                                                        new WaitAction(0.01),
                                                        new LambdaAction(
                                                                        () -> mLimelight.setShootingFromStage2Piece(true)))))));
                        mLimelight.setShootingFromStage2Piece(true);
                        //runAction(new WaitAction(0.1));
                        mSuperstructure.autoShot();
                }
                else {
                        runAction(new ParallelAction(List.of(
                                        driveToThirdNote3,
                                        new SeriesAction(List.of(
                                                        new LambdaAction(() -> Drive.getInstance()
                                                                        .setAutoHeading(Rotation2d.fromDegrees(160))),
                                                        new WaitAction(0.4),
                                                        new LambdaAction(() -> mSuperstructure
                                                                        .setSuperstuctureIntakingGround()),
                                                        new WaitAction(1.5),
                                                        new LambdaAction(
                                                                        () -> mSuperstructure.setSuperstuctureStow()))))));
                        mSuperstructure.setSuperstuctureStow();
                }

        }

        @Override
        public Pose2d getStartingPose() {
                Rotation2d startingRotation = Rotation2d.fromDegrees(240);
                if (Robot.is_red_alliance) {
                        startingRotation = Rotation2d.fromDegrees(300);
                }
                return new Pose2d(drivePath_C1.getInitialPose().getTranslation(), startingRotation);
        }
}
