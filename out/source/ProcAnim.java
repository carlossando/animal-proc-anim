/* autogenerated by Processing revision 1293 on 2024-07-16 */
import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import processing.javafx.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ProcAnim extends PApplet {



Fish fish;
Snake snake;
Lizard lizard;

int animal;

public void setup() {
  /* size commented out by preprocessor */;

  fish = new Fish(new PVector(width/2, height/2));
  snake = new Snake(new PVector(width/2, height/2));
  lizard = new Lizard(new PVector(width/2, height/2));

  animal = 0;
}

public void draw() {
  background(40, 44, 52);

  switch (animal) {
  case 0:
    fish.resolve();
    fish.display();
    fish.debugDisplay();
    break;
  case 1:
    snake.resolve();
    snake.display();
    snake.debugDisplay();
    break;
  case 2:
    lizard.resolve();
    lizard.display();
    lizard.debugDisplay();
    break;
  }
}

public void mousePressed() {
  if (++animal > 2) {
    animal = 0;
  }
}
class Chain {
  ArrayList<PVector> joints;
  int linkSize; // Space between joints

  // Only used in non-FABRIK resolution
  ArrayList<Float> angles;
  float angleConstraint; // Max angle diff between two adjacent joints, higher = loose, lower = rigid

  Chain(PVector origin, int jointCount, int linkSize) {
    this(origin, jointCount, linkSize, TWO_PI);
  }

  Chain(PVector origin, int jointCount, int linkSize, float angleConstraint) {
    this.linkSize = linkSize;
    this.angleConstraint = angleConstraint;
    joints = new ArrayList<>(); // Assumed to be >= 2, otherwise it wouldn't be much of a chain
    angles = new ArrayList<>();
    joints.add(origin.copy());
    angles.add(0f);
    for (int i = 1; i < jointCount; i++) {
      joints.add(PVector.add(joints.get(i - 1), new PVector(0, this.linkSize)));
      angles.add(0f);
    }
  }

  public void resolve(PVector pos) {
    angles.set(0, PVector.sub(pos, joints.get(0)).heading());
    joints.set(0, pos);
    for (int i = 1; i < joints.size(); i++) {
      float curAngle = PVector.sub(joints.get(i - 1), joints.get(i)).heading();
      angles.set(i, constrainAngle(curAngle, angles.get(i - 1), angleConstraint));
      joints.set(i, PVector.sub(joints.get(i - 1), PVector.fromAngle(angles.get(i)).setMag(linkSize)));
    }
  }

  public void fabrikResolve(PVector pos, PVector anchor) {
    // Forward pass
    joints.set(0, pos);
    for (int i = 1; i < joints.size(); i++) {
      joints.set(i, constrainDistance(joints.get(i), joints.get(i-1), linkSize));
    }

    // Backward pass
    joints.set(joints.size() - 1, anchor);
    for (int i = joints.size() - 2; i >= 0; i--) {
      joints.set(i, constrainDistance(joints.get(i), joints.get(i+1), linkSize));
    }
  }

  public void display() {
    strokeWeight(8);
    stroke(255);
    for (int i = 0; i < joints.size() - 1; i++) {
      PVector startJoint = joints.get(i);
      PVector endJoint = joints.get(i + 1);
      line(startJoint.x, startJoint.y, endJoint.x, endJoint.y);
    }

    fill(42, 44, 53);
    for (PVector joint : joints) {
      ellipse(joint.x, joint.y, 32, 32);
    }
  }
}
// Bloopy lil dude
class Fish {
  Chain spine;

  int bodyColor = color(58, 124, 165);
  int finColor = color(129, 195, 215);

  // Width of the fish at each vertabra
  float[] bodyWidth = {68, 81, 84, 83, 77, 64, 51, 38, 32, 19};

  Fish(PVector origin) {
    // 12 segments, first 10 for body, last 2 for caudal fin
    spine = new Chain(origin, 12, 64, PI/8);
  }

  public void resolve() {
    PVector headPos = spine.joints.get(0);
    PVector mousePos = new PVector(mouseX, mouseY);
    PVector targetPos = PVector.add(headPos, PVector.sub(mousePos, headPos).setMag(16));
    
    PVector absDist = new PVector(abs(mousePos.x - headPos.x), abs(mousePos.y - headPos.y));
    if(!(absDist.mag() < 20))
    {
      spine.resolve(targetPos);
    }
  }

  public void display() {
    strokeWeight(4);
    stroke(255);
    fill(finColor);

    // Alternate labels for shorter lines of code
    ArrayList<PVector> j = spine.joints;
    ArrayList<Float> a = spine.angles;

    // Relative angle differences are used in some hacky computation for the dorsal fin
    float headToMid1 = relativeAngleDiff(a.get(0), a.get(6));
    float headToMid2 = relativeAngleDiff(a.get(0), a.get(7));

    // For the caudal fin, we need to compute the relative angle difference from the head to the tail, but given
    // a joint count of 12 and angle constraint of PI/8, the maximum difference between head and tail is 11PI/8,
    // which is >PI. This complicates the relative angle calculation (flips the sign when curving too tightly).
    // A quick workaround is to compute the angle difference from the head to the middle of the fish, and then
    // from the middle of the fish to the tail.
    float headToTail = headToMid1 + relativeAngleDiff(a.get(6), a.get(11));

    // === START PECTORAL FINS ===
    pushMatrix();
    translate(getPosX(3, PI/3, 0), getPosY(3, PI/3, 0));
    rotate(a.get(2) - PI/4);
    ellipse(0, 0, 160, 64); // Right
    popMatrix();
    pushMatrix();
    translate(getPosX(3, -PI/3, 0), getPosY(3, -PI/3, 0));
    rotate(a.get(2) + PI/4);
    ellipse(0, 0, 160, 64); // Left
    popMatrix();
    // === END PECTORAL FINS ===

    // === START VENTRAL FINS ===
    pushMatrix();
    translate(getPosX(7, PI/2, 0), getPosY(7, PI/2, 0));
    rotate(a.get(6) - PI/4);
    ellipse(0, 0, 96, 32); // Right
    popMatrix();
    pushMatrix();
    translate(getPosX(7, -PI/2, 0), getPosY(7, -PI/2, 0));
    rotate(a.get(6) + PI/4);
    ellipse(0, 0, 96, 32); // Left
    popMatrix();
    // === END VENTRAL FINS ===

    // === START CAUDAL FINS ===
    beginShape();
    // "Bottom" of the fish
    for (int i = 8; i < 12; i++) {
      float tailWidth = 1.5f * headToTail * (i - 8) * (i - 8);
      curveVertex(j.get(i).x + cos(a.get(i) - PI/2) * tailWidth, j.get(i).y + sin(a.get(i) - PI/2) * tailWidth);
    }

    // "Top" of the fish
    for (int i = 11; i >= 8; i--) {
      float tailWidth = max(-13, min(13, headToTail * 6));
      curveVertex(j.get(i).x + cos(a.get(i) + PI/2) * tailWidth, j.get(i).y + sin(a.get(i) + PI/2) * tailWidth);
    }
    endShape(CLOSE);
    // === END CAUDAL FINS ===

    fill(bodyColor);

    // === START BODY ===
    beginShape();

    // Right half of the fish
    for (int i = 0; i < 10; i++) {
      curveVertex(getPosX(i, PI/2, 0), getPosY(i, PI/2, 0));
    }

    // Bottom of the fish
    curveVertex(getPosX(9, PI, 0), getPosY(9, PI, 0));

    // Left half of the fish
    for (int i = 9; i >= 0; i--) {
      curveVertex(getPosX(i, -PI/2, 0), getPosY(i, -PI/2, 0));
    }


    // Top of the head (completes the loop)
    curveVertex(getPosX(0, -PI/6, 0), getPosY(0, -PI/6, 0));
    curveVertex(getPosX(0, 0, 4), getPosY(0, 0, 4));
    curveVertex(getPosX(0, PI/6, 0), getPosY(0, PI/6, 0));

    // Some overlap needed because curveVertex requires extra vertices that are not rendered
    curveVertex(getPosX(0, PI/2, 0), getPosY(0, PI/2, 0));
    curveVertex(getPosX(1, PI/2, 0), getPosY(1, PI/2, 0));
    curveVertex(getPosX(2, PI/2, 0), getPosY(2, PI/2, 0));

    endShape(CLOSE);
    // === END BODY ===

    fill(finColor);

    // === START DORSAL FIN ===
    beginShape();
    vertex(j.get(4).x, j.get(4).y);
    bezierVertex(j.get(5).x, j.get(5).y, j.get(6).x, j.get(6).y, j.get(7).x, j.get(7).y);
    bezierVertex(j.get(6).x + cos(a.get(6) + PI/2) * headToMid2 * 16, j.get(6).y + sin(a.get(6) + PI/2) * headToMid2 * 16, j.get(5).x + cos(a.get(5) + PI/2) * headToMid1 * 16, j.get(5).y + sin(a.get(5) + PI/2) * headToMid1 * 16, j.get(4).x, j.get(4).y);
    endShape();
    // === END DORSAL FIN ===

    // === START EYES ===
    fill(255);
    ellipse(getPosX(0, PI/2, -18), getPosY(0, PI/2, -18), 24, 24);
    ellipse(getPosX(0, -PI/2, -18), getPosY(0, -PI/2, -18), 24, 24);
    // === END EYES ===
  }

  public void debugDisplay() {
    spine.display();
  }

  // Various helpers to shorten lines

  public float getPosX(int i, float angleOffset, float lengthOffset) {
    return spine.joints.get(i).x + cos(spine.angles.get(i) + angleOffset) * (bodyWidth[i] + lengthOffset);
  }

  public float getPosY(int i, float angleOffset, float lengthOffset) {
    return spine.joints.get(i).y + sin(spine.angles.get(i) + angleOffset) * (bodyWidth[i] + lengthOffset);
  }
}
// Glitchy lil dude
class Lizard {
  Chain spine;
  Chain[] arms;
  PVector[] armDesired;

  // Width of the lizard at each vertabra
  float[] bodyWidth = {52, 58, 40, 60, 68, 71, 65, 50, 28, 15, 11, 9, 7, 7};

  Lizard(PVector origin) {
    spine = new Chain(origin, 14, 64, PI/8);
    arms = new Chain[4];
    armDesired = new PVector[4];
    for (int i = 0; i < arms.length; i++) {
      arms[i] = new Chain(origin, 3, i < 2 ? 52 : 36);
      armDesired[i] = new PVector(0, 0);
    }
  }

  public void resolve() {
    PVector headPos = spine.joints.get(0);
    PVector mousePos = new PVector(mouseX, mouseY);
    PVector targetPos = PVector.add(headPos, PVector.sub(mousePos, headPos).setMag(12));

    PVector absDist = new PVector(abs(mousePos.x - headPos.x), abs(mousePos.y - headPos.y));
    if(!(absDist.mag() < 20))
    {
      spine.resolve(targetPos);
    }

    for (int i = 0; i < arms.length; i++) {
      int side = i % 2 == 0 ? 1 : -1;
      int bodyIndex = i < 2 ? 3 : 7;
      float angle = i < 2 ? PI/4 : PI/3;
      PVector desiredPos = new PVector(getPosX(bodyIndex, angle * side, 80), getPosY(bodyIndex, angle * side, 80));
      if (PVector.dist(desiredPos, armDesired[i]) > 200) {
        armDesired[i] = desiredPos;
      }

      arms[i].fabrikResolve(PVector.lerp(arms[i].joints.get(0), armDesired[i], 0.4f), new PVector(getPosX(bodyIndex, PI/2 * side, -20), getPosY(bodyIndex, PI/2 * side, -20)));
    }
  }

  public void display() {
    // === START ARMS ===
    noFill();
    for (int i = 0; i < arms.length; i++) {
      PVector shoulder = arms[i].joints.get(2);
      PVector foot = arms[i].joints.get(0);
      PVector elbow = arms[i].joints.get(1);
      // Doing a hacky thing to correct the back legs to be more physically accurate
      PVector para = PVector.sub(foot, shoulder);
      PVector perp = new PVector(-para.y, para.x).setMag(30);
      if (i == 2) {
        elbow = PVector.sub(elbow, perp);
      } else if (i == 3) {
        elbow = PVector.add(elbow, perp);
      }
      strokeWeight(40);
      stroke(255);
      bezier(shoulder.x, shoulder.y, elbow.x, elbow.y, elbow.x, elbow.y, foot.x, foot.y);
      strokeWeight(32);
      stroke(82, 121, 111);
      bezier(shoulder.x, shoulder.y, elbow.x, elbow.y, elbow.x, elbow.y, foot.x, foot.y);
    }
    // === END ARMS ===

    strokeWeight(4);
    stroke(255);
    fill(82, 121, 111);

    // === START BODY ===
    beginShape();

    // Right half of the lizard
    for (int i = 0; i < spine.joints.size(); i++) {
      curveVertex(getPosX(i, PI/2, 0), getPosY(i, PI/2, 0));
    }

    // Left half of the lizard
    for (int i = spine.joints.size() - 1; i >= 0; i--) {
      curveVertex(getPosX(i, -PI/2, 0), getPosY(i, -PI/2, 0));
    }


    // Top of the head (completes the loop)
    curveVertex(getPosX(0, -PI/6, -8), getPosY(0, -PI/6, -10));
    curveVertex(getPosX(0, 0, -6), getPosY(0, 0, -4));
    curveVertex(getPosX(0, PI/6, -8), getPosY(0, PI/6, -10));

    // Some overlap needed because curveVertex requires extra vertices that are not rendered
    curveVertex(getPosX(0, PI/2, 0), getPosY(0, PI/2, 0));
    curveVertex(getPosX(1, PI/2, 0), getPosY(1, PI/2, 0));
    curveVertex(getPosX(2, PI/2, 0), getPosY(2, PI/2, 0));

    endShape(CLOSE);
    // === END BODY ===

    // === START EYES ===
    fill(255);
    ellipse(getPosX(0, 3*PI/5, -7), getPosY(0, 3*PI/5, -7), 24, 24);
    ellipse(getPosX(0, -3*PI/5, -7), getPosY(0, -3*PI/5, -7), 24, 24);
    // === END EYES ===
  }

  public void debugDisplay() {
    spine.display();
  }

  public float getPosX(int i, float angleOffset, float lengthOffset) {
    return spine.joints.get(i).x + cos(spine.angles.get(i) + angleOffset) * (bodyWidth[i] + lengthOffset);
  }

  public float getPosY(int i, float angleOffset, float lengthOffset) {
    return spine.joints.get(i).y + sin(spine.angles.get(i) + angleOffset) * (bodyWidth[i] + lengthOffset);
  }
}
// Wiggly lil dude
class Snake {
  Chain spine;

  Snake(PVector origin) {
    spine = new Chain(origin, 48, 64, PI/8);
  }

  public void resolve() {
    PVector headPos = spine.joints.get(0);
    PVector mousePos = new PVector(mouseX, mouseY);
    PVector targetPos = PVector.add(headPos, PVector.sub(mousePos, headPos).setMag(8));
    
    PVector absDist = new PVector(abs(mousePos.x - headPos.x), abs(mousePos.y - headPos.y));
    if(!(absDist.mag() < 20))
    {
      spine.resolve(targetPos);
    }
  }

  public void display() {
    strokeWeight(4);
    stroke(255);
    fill(172, 57, 49);

    // === START BODY ===
    beginShape();

    // Right half of the snake
    for (int i = 0; i < spine.joints.size(); i++) {
      curveVertex(getPosX(i, PI/2, 0), getPosY(i, PI/2, 0));
    }

    curveVertex(getPosX(47, PI, 0), getPosY(47, PI, 0));

    // Left half of the snake
    for (int i = spine.joints.size() - 1; i >= 0; i--) {
      curveVertex(getPosX(i, -PI/2, 0), getPosY(i, -PI/2, 0));
    }


    // Top of the head (completes the loop)
    curveVertex(getPosX(0, -PI/6, 0), getPosY(0, -PI/6, 0));
    curveVertex(getPosX(0, 0, 0), getPosY(0, 0, 0));
    curveVertex(getPosX(0, PI/6, 0), getPosY(0, PI/6, 0));

    // Some overlap needed because curveVertex requires extra vertices that are not rendered
    curveVertex(getPosX(0, PI/2, 0), getPosY(0, PI/2, 0));
    curveVertex(getPosX(1, PI/2, 0), getPosY(1, PI/2, 0));
    curveVertex(getPosX(2, PI/2, 0), getPosY(2, PI/2, 0));

    endShape(CLOSE);
    // === END BODY ===

    // === START EYES ===
    fill(255);
    ellipse(getPosX(0, PI/2, -18), getPosY(0, PI/2, -18), 24, 24);
    ellipse(getPosX(0, -PI/2, -18), getPosY(0, -PI/2, -18), 24, 24);
    // === END EYES ===
  }

  public void debugDisplay() {
    spine.display();
  }

  public float bodyWidth(int i) {
    switch(i) {
    case 0:
      return 76;
    case 1:
      return 80;
    default:
      return 64 - i;
    }
  }

  public float getPosX(int i, float angleOffset, float lengthOffset) {
    return spine.joints.get(i).x + cos(spine.angles.get(i) + angleOffset) * (bodyWidth(i) + lengthOffset);
  }

  public float getPosY(int i, float angleOffset, float lengthOffset) {
    return spine.joints.get(i).y + sin(spine.angles.get(i) + angleOffset) * (bodyWidth(i) + lengthOffset);
  }
}
// Constrain the vector to be at a certain range of the anchor
public PVector constrainDistance(PVector pos, PVector anchor, float constraint) {
  return PVector.add(anchor, PVector.sub(pos, anchor).setMag(constraint));
}

// Constrain the angle to be within a certain range of the anchor
public float constrainAngle(float angle, float anchor, float constraint) {
  if (abs(relativeAngleDiff(angle, anchor)) <= constraint) {
    return simplifyAngle(angle);
  }

  if (relativeAngleDiff(angle, anchor) > constraint) {
    return simplifyAngle(anchor - constraint);
  }

  return simplifyAngle(anchor + constraint);
}

// i.e. How many radians do you need to turn the angle to match the anchor?
public float relativeAngleDiff(float angle, float anchor) {
  // Since angles are represented by values in [0, 2pi), it's helpful to rotate
  // the coordinate space such that PI is at the anchor. That way we don't have
  // to worry about the "seam" between 0 and 2pi.
  angle = simplifyAngle(angle + PI - anchor);
  anchor = PI;

  return anchor - angle;
}

// Simplify the angle to be in the range [0, 2pi)
public float simplifyAngle(float angle) {
  while (angle >= TWO_PI) {
    angle -= TWO_PI;
  }

  while (angle < 0) {
    angle += TWO_PI;
  }

  return angle;
}


  public void settings() { fullScreen(FX2D); }

  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "ProcAnim" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
