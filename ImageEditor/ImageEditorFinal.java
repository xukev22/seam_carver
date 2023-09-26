import java.util.ArrayList;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;
import java.awt.Color;

// TODO: final run (tick presentation)/formatting, user guide ZIP IMAGES!!!

// controls the animation of seam removals
class ImageEditor extends World {

  // the end of the list represents the most recently removed seam
  ArrayList<SeamInfo> seamHistory;

  // random variable based on a seed (consistently generates same random sequence
  // if same seed)
  Random rand;

  // are we removing vertically or horizontally?
  boolean removeVertical = true;

  // should the seam be removed, or should it display a red line?
  boolean removeSeam = false;

  // is the game paused
  boolean paused = false;

  // override the randomness component and queue a vertical
  // seam removal
  boolean removeVerticalOverride = false;

  // override the randomness component and queue a
  // horizontal seam removal
  boolean removeHorizontalOverride = false;

  // show gray scale based off energy?
  boolean grayScaleOffEnergy = false;

  // is the game inflating?
  boolean inflating = false;

  Grid grid;

  ImageEditor(Grid grid) {
    this.grid = grid;
    this.rand = new Random(420);
    this.seamHistory = new ArrayList<>();
  }

  // creates the scene that visualizes the pixels in the grid according to the
  // world parameters
  public WorldScene makeScene() {
    WorldScene w = new WorldScene(1000, 1000);
    w.placeImageXY(this.grid.makeImage(this.grayScaleOffEnergy), 500, 500);
    return w;
  }

  // EFFECT: mutates the world state based on given the key press
  public void onKeyEvent(String key) {
    // does not process the image once it is smaller than 3 on either dimension
    // grid construction assumes at least a 2x2 grid
    if (this.grid.rootPixel.rowCount() < 3 || this.grid.rootPixel.colCount() < 3) {
      // unless we are trying to inflate, then we will allow r to be pressed
      if (!key.equals("r")) {
        return;
      }
    }

    // Extra Credit:
    // if the space bar is pressed, pause the game
    if (key.equals(" ")) {
      this.paused = !this.paused;
    }

    // Extra Credit: User can choose to remove a vertical or horizontal seam with
    // "v" or "h" keypresses, respectively
    // Rather than another call to grid.removeVerticalSeam(), we interpret the key
    // press as simply
    // overriding the next tick of random selection to remove either a vertical or
    // horizontal seam
    else if (key.equals("v")) {
      this.removeVerticalOverride = true;
      this.removeHorizontalOverride = false;
    }
    else if (key.equals("h")) {
      this.removeHorizontalOverride = true;
      this.removeVerticalOverride = false;
    }

    // Extra Credit: User can choose to toggle a gray scale view of the pixel
    // energies
    else if (key.equals("g")) {
      this.grayScaleOffEnergy = !this.grayScaleOffEnergy;
    }

    // Extra Credit: User can choose to toggle between normal and reverse direction
    // of
    // the seams removed, reinserting them back into the image
    else if (key.equals("r")) {
      this.inflating = !this.inflating;
    }
    else {
      return;
    }
  }

  // EFFECT: mutates the the world state on each tick
  public void onTick() {

    // if the game is paused do not run any code
    if (this.paused) {
      return;
    }

    // does not process the image once it is smaller than 3 on either dimension
    if (this.grid.rootPixel.rowCount() < 3 || this.grid.rootPixel.colCount() < 3) {
      // unless we are reversing, then we will keep running code in onTick
      if (!this.inflating) {
        return;
      }

    }

    if (this.inflating) {
      // we are inflating
      if (this.seamHistory.size() == 0) {
        // make sure history is not empty, if it is, do nothing
        return;
      }

      if (this.removeSeam) {
        // also if we are on a red line, remove the correct seam and the red line
        if (this.removeVertical) {
          // vertical red line
          this.seamHistory.add(this.grid.findLowestSeamVertical());
          this.grid.removeSeamVertical(false);
        }
        else {
          // horizontal red line
          this.seamHistory.add(this.grid.findLowestSeamHorizontal());
          this.grid.removeSeamHorizontal(false);
        }

        // next removal of seam will be a red line
        this.removeSeam = false;

      }

      // EFFECT: removes the last element in the seam history and stores it in
      // seamToInsert
      SeamInfo seamToInsert = this.seamHistory.remove(this.seamHistory.size() - 1);

      this.grid.reinsertSeam(seamToInsert);

      // dont do anything else on this tick
      return;
    }

    if (!this.removeSeam) {
      // drawing a red line case

      if (this.removeHorizontalOverride || this.removeVerticalOverride) {
        // if a remove hor/vertical seam has been queued (to remove a single seam)
        // we will not draw a red line, and simply remove the queued seam

        if (this.removeHorizontalOverride) {
          // horiontal red line
          this.seamHistory.add(this.grid.findLowestSeamHorizontal());
          this.grid.removeSeamHorizontal(false);
        }
        else {
          // vertical red line
          this.seamHistory.add(this.grid.findLowestSeamVertical());
          this.grid.removeSeamVertical(false);
        }

        // reset the overrides
        this.removeVerticalOverride = false;
        this.removeHorizontalOverride = false;

        return;
      }

      // randomly generates a number that is either 0 or 1, if the number is 1,
      // highlight a vertical seam, otherwise, a horizontal seam
      if (this.rand.nextInt(2) == 1) {
        this.removeVertical = true;
        this.grid.removeSeamVertical(true);
      }
      else {
        this.removeVertical = false;
        this.grid.removeSeamHorizontal(true);
      }
    }
    else {
      // in the process of removing a seam on this tick
      if (this.removeVertical) {
        // vertical seam to remove
        this.seamHistory.add(this.grid.findLowestSeamVertical());
        this.grid.removeSeamVertical(false);
      }
      else {
        // horizontal seam to remove
        this.seamHistory.add(this.grid.findLowestSeamHorizontal());
        this.grid.removeSeamHorizontal(false);
      }

    }

    // switch red line toggle
    this.removeSeam = !this.removeSeam;
  }

}

// a linked-list describing all the pixels in the seam
class SeamInfo {
  Pixel pixel;
  double totalWeight;
  // base case, cameFrom is null (first pixel in the seam)
  SeamInfo cameFrom;

  SeamInfo(Pixel pixel, double totalWeight, SeamInfo cameFrom) {
    this.pixel = pixel;
    this.totalWeight = totalWeight;
    this.cameFrom = cameFrom;
  }

  // EFFECT: either adds the red color to this pixel or removes the pixel in this
  // row and fixes up the connections in the grid, if the rootPixel is modified,
  // returns the new rootPixel,
  // otherwise returns null
  APixel removeInRow(boolean redLine) {

    APixel newRoot = null;

    if (!this.pixel.isWellFormedRow()) {
      throw new RuntimeException("Row not well formed before removal!");
    }

    if (redLine) {
      this.pixel.maskedColor = redLine;
    }
    else {

      if (this.cameFrom == null) {
        // we are at the top and do not need to fix anything above
      }

      else if (this.cameFrom.pixel == this.pixel.getTop()) {
        // the next pixel to remove is right above us and we do not need to fix any
        // diagonal connections
      }
      else if (this.cameFrom.pixel == this.pixel.getTop().getLeft()) {
        // the next pixel is up and to the left so we need to set left's top neighbor to
        // be the pixel above this pixel (and vise versa)

        this.pixel.getLeft().changeNeighbor(this.pixel.getTop(), 0);
        this.pixel.getTop().changeNeighbor(this.pixel.getLeft(), 2);
      }
      else if (this.cameFrom.pixel == this.pixel.getTop().getRight()) {
        // the next pixel is up and to the right so we need to set right's top neighbor
        // to be the pixel above this pixel (and vise versa)

        this.pixel.getRight().changeNeighbor(this.pixel.getTop(), 0);
        this.pixel.getTop().changeNeighbor(this.pixel.getRight(), 2);
      }

      // always make the left and right pixels point to each other to remove this
      // pixel

      if (this.pixel.getLeft().isBorder() && this.pixel.getTop().isBorder()) {
        // change the new root if the rootPixel is removed (part of the seam)
        newRoot = this.pixel.getRight();
      }

      this.pixel.getLeft().changeNeighbor(this.pixel.getRight(), 1);
      this.pixel.getRight().changeNeighbor(this.pixel.getLeft(), 3);

    }

    return newRoot;
  }

  // EFFECT: either adds the red color to this pixel or removes the pixel in this
  // col and fixes up the connections in the grid, if the rootPixel is modified,
  // returns the new rootPixel,
  // otherwise returns null
  APixel removeInCol(boolean redLine) {

    APixel newRoot = null;

    if (!this.pixel.isWellFormedCol()) {
      throw new RuntimeException("Col not well formed before removal!");
    }

    if (redLine) {
      this.pixel.maskedColor = redLine;
    }
    else {

      if (this.cameFrom == null) {
        // we are at the left and do not need to fix anything to the left
      }
      else if (this.cameFrom.pixel == this.pixel.getLeft()) {
        // the next pixel to remove is directly left of us and we do not need to fix any
        // diagonal connections
      }
      else if (this.cameFrom.pixel == this.pixel.getTop().getLeft()) {
        // the next pixel is up and to the left so we need to set the pixel above this
        // pixel to refer to the pixel below and to the left of it (and vise versa)

        this.pixel.getTop().changeNeighbor(this.pixel.getLeft(), 3);
        this.pixel.getLeft().changeNeighbor(this.pixel.getTop(), 1);
      }
      else if (this.cameFrom.pixel == this.pixel.getBottom().getLeft()) {
        // the next pixel is down and to the left so we need to set the pixel below this
        // pixel to refer to the pixel above and to the left of it (and vise versa)

        this.pixel.getBottom().changeNeighbor(this.pixel.getLeft(), 3);
        this.pixel.getLeft().changeNeighbor(this.pixel.getBottom(), 1);
      }

      if (this.pixel.getLeft().isBorder() && this.pixel.getTop().isBorder()) {
        // change the new root if the rootPixel is removed (part of the seam)
        newRoot = this.pixel.getBottom();
      }

      // always make the top and bottom pixels point to each other to remove this
      // pixel
      this.pixel.getTop().changeNeighbor(this.pixel.getBottom(), 2);
      this.pixel.getBottom().changeNeighbor(this.pixel.getTop(), 0);

    }
    return newRoot;
  }

  // EFFECT: reinserts this SeamInfo's pixel into the grid, also returning the
  // pixel if the pixel is the new rootPixel after reinserting, otherwise returns
  // null
  APixel reinsertSeamHelp() {

    // reset red line
    this.pixel.maskedColor = false;

    APixel newRoot = null;

    APixel curPixel = this.pixel;
    curPixel.getRight().changeNeighbor(curPixel, 3);
    curPixel.getLeft().changeNeighbor(curPixel, 1);
    curPixel.getTop().changeNeighbor(curPixel, 2);
    curPixel.getBottom().changeNeighbor(curPixel, 0);

    curPixel.getRight().changeNeighbor(curPixel.getTop().getRight(), 0);
    curPixel.getBottom().changeNeighbor(curPixel.getBottom().getLeft(), 3);

    curPixel.getTop().changeNeighbor(curPixel.getTop().getLeft(), 3);
    curPixel.getLeft().changeNeighbor(curPixel.getTop().getLeft(), 0);

    if (curPixel.getTop().isBorder() && curPixel.getLeft().isBorder()) {
      newRoot = curPixel;
    }

    if (!this.pixel.isWellFormedRow() || !this.pixel.isWellFormedCol()) {
      throw new RuntimeException("Row or col not well formed after reinsertion!");
    }

    return newRoot;

  }

}

// represents a connection of pixels and their neighbors to form an overall
// image
class Grid {
  // the top left pixel in an image, is always present
  Pixel rootPixel;

  // EFFECT: reinserts the given seam by fixing up all connections to this seam,
  // and making sure the grid is still well formed after removal
  void reinsertSeam(SeamInfo seam) {

    if (seam == null) {
      return;
    }

    APixel newRoot = seam.reinsertSeamHelp();
    if (newRoot != null) {
      this.rootPixel = newRoot.asPixel();
    }

    this.reinsertSeam(seam.cameFrom);
  }

  // returns the world image that visualizes this grid
  ComputedPixelImage makeImage(boolean grayScale) {

    int width = this.rootPixel.colCount();
    int height = this.rootPixel.rowCount();
    ComputedPixelImage img = new ComputedPixelImage(width, height);
    APixel current = this.rootPixel;
    int y = 0;
    double maxEnergy = this.getMaxEnergy();
    // INVARIANT: isBorder() returns true when it is called on a BorderPixel
    // every pixel has a finite amount of neighbors in either direction
    // eventually by continually calling getBottom, a BorderPixel will be reached
    // and isBorder() will return true, so the while loop will terminate

    // EFFECT: mutates the computed pixel image row by row and increases y,
    // updating the row and the currentPixel
    while (!current.isBorder()) {
      if (y < height) {
        this.makeImageRow(current, img, width, y, grayScale, maxEnergy);
        current = current.getBottom();
        y += 1;
      }
    }

    return img;

  }

  // returns the maximum energy of a single pixel in the grid
  double getMaxEnergy() {
    double maximum = this.rootPixel.energy();
    return this.rootPixel.getMaximumEnergyGrid(maximum);
  }

  // EFFECT: mutates the ComputedPixelImage in the given y row
  void makeImageRow(APixel pixel, ComputedPixelImage img, int width, int y, boolean grayScale,
      double maxEnergy) {
    APixel current = pixel;
    int x = 0;

    // INVARIANT: isBorder() returns true when it is called on a BorderPixel
    // every pixel has a finite amount of neighbors in either direction
    // eventually by continually calling getRight, a BorderPixel will be reached
    // and isBorder() will return true, so the while loop will terminate

    // EFFECT: mutates the pixel at the given x and y to the currentPixel's color,
    // updating the pixel and the x index
    while (!current.isBorder()) {
      if (x < width) {
        if (grayScale) {
          img.setPixel(x, y, current.getGrayScale(maxEnergy));

        }
        else {
          img.setPixel(x, y, current.getColor());

        }
        current = current.getRight();
        x += 1;
      }
    }
  }

  // constructs the grid by connecting all the pixels to each other and setting
  // the rootPixel to the pixel at row 0 col 0
  Grid(FromFileImage image) {
    ArrayList<ArrayList<Pixel>> pixels = new ArrayList<>();

    // initializes all of the rows in the 2d arraylist of pixels
    for (int row = 0; row < image.getHeight(); row += 1) {
      pixels.add(new ArrayList<Pixel>());
    }

    // row: 0 is the top row, increasing row index means going down
    // col: 0 is the left column, increasing col index means going right

    // adds the pixel to its corresponding spot in the 2d arraylist of pixels from
    // the image
    for (int row = 0; row < image.getHeight(); row += 1) {
      // adds the pixel to the given row and col
      for (int col = 0; col < image.getWidth(); col += 1) {

        pixels.get(row).add(new Pixel(image.getColorAt(col, row)));

      }
    }

    // assigns every pixel its correct neighbors
    for (int row = 0; row < image.getHeight(); row += 1) {
      // get the pixel at the given row and col and set its neighbors
      // based on its position in the image (corners, edges, middle)
      for (int col = 0; col < image.getWidth(); col += 1) {
        Pixel pixel = pixels.get(row).get(col);

        // EFFECT: connects all the pixels
        this.connectPixels(image, pixels, row, col, pixel);
      }
    }

  }

  void connectPixels(FromFileImage image, ArrayList<ArrayList<Pixel>> pixels, int row, int col,
      Pixel pixel) {

    // assumes at least 2x2 grid
    // 7 cases for a pixel in the image:

    // 4 corners
    // 2 edge pairs [top/bottom] (not including corners)
    // pixel not in edge

    // overlap of setting nodes does occur

    if (row == 0 && col == 0) {

      this.rootPixel = pixel;

      // case top left corner pixel (root pixel)

      Pixel pixelRight = pixels.get(row).get(col + 1);
      Pixel pixelBottom = pixels.get(row + 1).get(col);

      pixel.setNeighbor(pixelRight, 1);
      pixel.setNeighbor(pixelBottom, 2);
    }
    else if (row == 0 && col == image.getWidth() - 1) {
      // case top right corner pixel

      Pixel pixelLeft = pixels.get(row).get(col - 1);
      Pixel pixelBottom = pixels.get(row + 1).get(col);

      pixel.setNeighbor(pixelLeft, 3);
      pixel.setNeighbor(pixelBottom, 2);
    }
    else if (row == image.getHeight() - 1 && col == 0) {
      // case bottom left corner pixel

      Pixel pixelRight = pixels.get(row).get(col + 1);
      Pixel pixelTop = pixels.get(row - 1).get(col);

      pixel.setNeighbor(pixelRight, 1);
      pixel.setNeighbor(pixelTop, 0);
    }
    else if (row == image.getHeight() - 1 && col == image.getWidth() - 1) {
      // case bottom right corner pixel

      Pixel pixelLeft = pixels.get(row).get(col - 1);
      Pixel pixelTop = pixels.get(row - 1).get(col);

      pixel.setNeighbor(pixelLeft, 3);
      pixel.setNeighbor(pixelTop, 0);
    }
    else if (row == 0 || row == image.getHeight() - 1) {
      // case top/bottom row of pixel (not corners)

      Pixel pixelLeft = pixels.get(row).get(col - 1);
      Pixel pixelRight = pixels.get(row).get(col + 1);

      pixel.setNeighbor(pixelLeft, 3);
      pixel.setNeighbor(pixelRight, 1);
    }
    else if (col == 0 || col == image.getWidth() - 1) {
      // case left/right col of pixel (not corners)

      Pixel pixelTop = pixels.get(row - 1).get(col);
      Pixel pixelBottom = pixels.get(row + 1).get(col);

      pixel.setNeighbor(pixelTop, 0);
      pixel.setNeighbor(pixelBottom, 2);
    }
    else {
      // case not on the perimeter of the image

      Pixel pixelLeft = pixels.get(row).get(col - 1);
      Pixel pixelTop = pixels.get(row - 1).get(col);
      Pixel pixelRight = pixels.get(row).get(col + 1);
      Pixel pixelBottom = pixels.get(row + 1).get(col);

      pixel.setNeighbor(pixelRight, 1);
      pixel.setNeighbor(pixelBottom, 2);
      pixel.setNeighbor(pixelLeft, 3);
      pixel.setNeighbor(pixelTop, 0);

    }
  }

  // EFFECT: removes the lowest energy seam vertically in the grid of pixels,
  // fixing all the connections, and making sure the grid is still well formed
  // after removal
  void removeSeamVertical(boolean redLine) {
    SeamInfo lowestSeam = this.findLowestSeamVertical();
    this.removeSeamVerticalHelp(lowestSeam, redLine);

  }

  // EFFECT: removes the lowest energy seam horizontally in the grid of pixels,
  // fixing all the connections, and making sure the grid is still well formed
  // after removal
  void removeSeamHorizontal(boolean redLine) {
    SeamInfo lowestSeam = this.findLowestSeamHorizontal();
    this.removeSeamHorizontalHelp(lowestSeam, redLine);
  }

  // EFFECT: removes the lowest energy seam vertically in the grid of pixels,
  // fixing all the
  // connections, accumulating the lowestSeam, by traversing back the
  // linked-list until it is empty (null)
  void removeSeamVerticalHelp(SeamInfo lowestSeam, boolean redLine) {
    if (lowestSeam == null) {
      return;
    }

    APixel newRoot = lowestSeam.removeInRow(redLine);
    if (newRoot != null) {
      this.rootPixel = newRoot.asPixel();
    }
    this.removeSeamVerticalHelp(lowestSeam.cameFrom, redLine);
  }

  // EFFECT: removes the lowest energy seam horizontally in the grid of pixels,
  // fixing all the
  // connections, accumulating the lowestSeam, by traversing back the
  // linked-list until it is empty (null)
  void removeSeamHorizontalHelp(SeamInfo lowestSeam, boolean redLine) {
    if (lowestSeam == null) {
      return;
    }

    APixel newRoot = lowestSeam.removeInCol(redLine);
    if (newRoot != null) {
      this.rootPixel = newRoot.asPixel();
    }
    this.removeSeamHorizontalHelp(lowestSeam.cameFrom, redLine);
  }

  // returns the seam with the lowest total energy
  SeamInfo findLowestSeamVertical() {

    ArrayList<SeamInfo> currentSeams = new ArrayList<>();

    // assigns every pixel in the first row to a new seam info
    for (int col = 0; col < this.rootPixel.colCount(); col += 1) {
      Pixel currentPixel = this.rootPixel.getPixelAt(0, col);
      currentSeams.add(new SeamInfo(currentPixel, currentPixel.energy(), null));
    }

    // EFFECT:
    // iterates through every subsequent row of pixels and mutates currentSeams
    // based on the original value and the new row of weights
    for (int row = 1; row < this.rootPixel.rowCount(); row += 1) {
      Pixel leftMostPixelInRow = this.rootPixel.getPixelAt(row, 0);
      currentSeams = findLowestSeamHelp(currentSeams, leftMostPixelInRow.constructPixelRow());

    }

    return this.findMinSeamEnergy(currentSeams);

  }

  // returns the seam with the lowest total energy
  SeamInfo findLowestSeamHorizontal() {

    ArrayList<SeamInfo> currentSeams = new ArrayList<>();

    // assigns every pixel in the first row to a new seam info
    for (int row = 0; row < this.rootPixel.rowCount(); row += 1) {
      Pixel currentPixel = this.rootPixel.getPixelAt(row, 0);
      currentSeams.add(new SeamInfo(currentPixel, currentPixel.energy(), null));
    }

    // EFFECT:
    // iterates through every subsequent col of pixels and mutates currentSeams
    // based on the original value and the new col of weights
    for (int col = 1; col < this.rootPixel.colCount(); col += 1) {
      Pixel topMostPixelInCol = this.rootPixel.getPixelAt(0, col);
      currentSeams = findLowestSeamHelp(currentSeams, topMostPixelInCol.constructPixelCol());

    }

    return this.findMinSeamEnergy(currentSeams);

  }

  // appends a new row of pixels onto the given current seam based on the row of
  // pixels that are below it
  ArrayList<SeamInfo> findLowestSeamHelp(ArrayList<SeamInfo> currSeam,
      ArrayList<Pixel> belowPixelRowOrCol) {
    ArrayList<SeamInfo> nextSeams = new ArrayList<>();

    // EFFECT:
    // iterates through the pixels in the next row of pixels and generates a new
    // list of seams with these pixels appended to the list
    for (int pixelIndex = 0; pixelIndex < belowPixelRowOrCol.size(); pixelIndex += 1) {
      ArrayList<SeamInfo> neighboringSeams = new ArrayList<>();

      neighboringSeams.add(currSeam.get(pixelIndex));

      if (pixelIndex == 0) {
        neighboringSeams.add(currSeam.get(pixelIndex + 1));
      }
      else if (pixelIndex == belowPixelRowOrCol.size() - 1) {
        neighboringSeams.add(currSeam.get(pixelIndex - 1));
      }
      else {
        neighboringSeams.add(currSeam.get(pixelIndex + 1));
        neighboringSeams.add(currSeam.get(pixelIndex - 1));
      }

      SeamInfo lowestSeam = findMinSeamEnergy(neighboringSeams);

      nextSeams.add(new SeamInfo(belowPixelRowOrCol.get(pixelIndex),
          lowestSeam.totalWeight + belowPixelRowOrCol.get(pixelIndex).energy(), lowestSeam));
    }

    return nextSeams;
  }

  // returns the SeamInfo with the lowest weight
  SeamInfo findMinSeamEnergy(ArrayList<SeamInfo> seams) {
    SeamInfo minSeamSoFar = seams.get(0);

    // EFFECT:
    // iterates through the seams and mutates minSeamSoFar to refer to the new seam
    // if it is smaller
    for (int seamIndex = 0; seamIndex < seams.size(); seamIndex += 1) {
      if (seams.get(seamIndex).totalWeight < minSeamSoFar.totalWeight) {
        minSeamSoFar = seams.get(seamIndex);
      }

    }

    return minSeamSoFar;
  }

}

// represents either a pixel or a border pixel
interface IPixel {

  // returns the gray scale equivalent of a pixel when represented using its
  // energy instead of its color
  Color getGrayScale(double maxEnergy);

  // returns the pixel version of the abstract pixel, throwing an exception if a
  // border pixel calls this
  Pixel asPixel();

  // gets the color of this APixel (a border pixel has no actual color and throws
  // exception)
  Color getColor();

  // returns whether or not the pixel is out of bounds (outside the image)
  boolean isBorder();

  // EFFECT: sets this APixel's neighbor to the given new pixel,
  // where 0 represents top, 1 represents right, 2 represents bottom, 3 represents
  // left
  void changeNeighbor(APixel newPixel, int direction);

  // returns the amount of rows below this pixel (including this pixel)
  int rowCount();

  // returns the amount of columns to the right of this pixel (including this
  // pixel)
  int colCount();

  // returns the pixel at the given row and col offset with respect to this pixel
  // 0,0 means same pixel
  // only traverses down and right
  // rowOffset of 3 means down 3 rows
  // colOffset of 3 means right 3 cols
  Pixel getPixelAt(int colOffset, int rowOffset);

  // EFFECT:
  // returns an array list of each pixel to the right of this pixel (including
  // this pixel), accumulating the list of pixels encountered so far
  ArrayList<Pixel> constructPixelRowHelp(ArrayList<Pixel> listSoFar);

  // EFFECT:
  // returns an array list of each pixel below this pixel (including
  // this pixel), accumulating the list of pixels encountered so far
  ArrayList<Pixel> constructPixelColHelp(ArrayList<Pixel> listSoFar);

  // returns the pixel above this pixel or a border pixel if this pixel is at the
  // top of the image
  APixel getTop();

  // returns the pixel to the right of this pixel or a border pixel if this pixel
  // is at the right of the image
  APixel getRight();

  // returns the pixel below this pixel or a border pixel if this pixel is at the
  // bottom of the image
  APixel getBottom();

  // returns the pixel to the left of this pixel or a border pixel if this pixel
  // is at the left of the image
  APixel getLeft();

  // computes the "brightness" of this pixel
  double brightness();

  // calculates the horizontal energy of this pixel
  double hEnergy();

  // calculates the vertical energy of this pixel
  double vEnergy();

  // calculates the overall energy of this pixel
  double energy();

  // returns the energy of the pixel with highest energy in this row
  double getMaximumEnergyRow(double currMax);

  // returns the energy of the pixel with highest energy in this grid
  double getMaximumEnergyGrid(double currMax);
}

// represents a pixel that either borders the image or is inside the image
abstract class APixel implements IPixel {
  Color color;

  APixel(Color color) {
    this.color = color;
  }

  // returns the gray scale equivalent of a pixel when represented using its
  // energy instead of its color
  public abstract Color getGrayScale(double maxEnergy);

  // returns the pixel version of the abstract pixel, throwing an exception if a
  // border pixel calls this
  public abstract Pixel asPixel();

  // gets the color of this APixel (a border pixel has no actual color and throws
  // exception)
  public abstract Color getColor();

  // returns whether or not the pixel is out of bounds (outside the image)
  public abstract boolean isBorder();

  // EFFECT: sets this APixel's neighbor to the given new pixel,
  // where 0 represents top, 1 represents right, 2 represents bottom, 3 represents
  // left
  public abstract void changeNeighbor(APixel newPixel, int direction);

  // returns the amount of rows below this pixel (including this pixel)
  public abstract int rowCount();

  // returns the amount of columns to the right of this pixel (including this
  // pixel)
  public abstract int colCount();

  // returns the pixel at the given row and col offset with respect to this pixel
  // 0,0 means same pixel
  // only traverses down and right
  // rowOffset of 3 means down 3 rows
  // colOffset of 3 means right 3 cols
  public abstract Pixel getPixelAt(int colOffset, int rowOffset);

  // EFFECT:
  // returns an array list of each pixel to the right of this pixel (including
  // this pixel), accumulating the list of pixels encountered so far
  public abstract ArrayList<Pixel> constructPixelRowHelp(ArrayList<Pixel> listSoFar);

  // EFFECT:
  // returns an array list of each pixel below this pixel (including
  // this pixel), accumulating the list of pixels encountered so far
  public abstract ArrayList<Pixel> constructPixelColHelp(ArrayList<Pixel> listSoFar);

  // returns the pixel above this pixel or a border pixel if this pixel is at the
  // top of the image
  public abstract APixel getTop();

  // returns the pixel to the right of this pixel or a border pixel if this pixel
  // is at the right of the image
  public abstract APixel getRight();

  // returns the pixel below this pixel or a border pixel if this pixel is at the
  // bottom of the image
  public abstract APixel getBottom();

  // returns the pixel to the left of this pixel or a border pixel if this pixel
  // is at the left of the image
  public abstract APixel getLeft();

  // computes the "brightness" of this pixel
  public double brightness() {
    return (this.color.getRed() + this.color.getBlue() + this.color.getGreen()) / (3.0 * 255.0);
  }

  // calculates the horizontal energy of this pixel
  public double hEnergy() {
    double topLeftBr = this.getTop().getLeft().brightness();
    double leftBr = this.getLeft().brightness();
    double bottomLeftBr = this.getBottom().getLeft().brightness();

    double topRightBr = this.getTop().getRight().brightness();
    double rightBr = this.getRight().brightness();
    double bottomRightBr = this.getBottom().getRight().brightness();

    return (topLeftBr + 2.0 * leftBr + bottomLeftBr) - (topRightBr + 2.0 * rightBr + bottomRightBr);
  }

  // calculates the vertical energy of this pixel
  public double vEnergy() {
    double topLeftBr = this.getTop().getLeft().brightness();
    double topBr = this.getTop().brightness();
    double topRightBr = this.getTop().getRight().brightness();

    double bottomLeftBr = this.getBottom().getLeft().brightness();
    double bottomBr = this.getBottom().brightness();
    double bottomRightBr = this.getBottom().getRight().brightness();

    return (topLeftBr + 2.0 * topBr + topRightBr) - (bottomLeftBr + 2.0 * bottomBr + bottomRightBr);
  }

  // calculates the overall energy of this pixel
  public double energy() {
    return Math.sqrt((this.hEnergy() * this.hEnergy() + this.vEnergy() * this.vEnergy()));
  }

  // returns the energy of the pixel with highest energy in this row
  public abstract double getMaximumEnergyRow(double currMax);

  // returns the energy of the pixel with highest energy in this grid
  public abstract double getMaximumEnergyGrid(double currMax);

}

// represents a pixel that is within the image
class Pixel extends APixel {

  // the masked color of this pixel for highlighting a path (not the actual
  // color)
  boolean maskedColor = false;

  APixel top;
  APixel right;
  APixel bottom;
  APixel left;

  Pixel(Color color) {
    super(color);

    this.top = new BorderPixel();
    this.right = new BorderPixel();
    this.bottom = new BorderPixel();
    this.left = new BorderPixel();
  }

  // checks whether a pixel is well formed in the col
  boolean isWellFormedCol() {
    if (this.getTop().isBorder()) {
      if (this.getBottom().getTop() != this) {
        return false;
      }
    }
    else if (this.getBottom().isBorder()) {
      if (this.getTop().getBottom() != this) {
        return false;
      }
    }
    else if (this.getTop().getBottom() != this || this.getBottom().getTop() != this) {
      return false;
    }
    return true;
  }

  // checks whether a pixel is well formed in the row
  boolean isWellFormedRow() {
    if (this.getRight().isBorder()) {
      if (this.getLeft().getRight() != this) {
        return false;
      }
    }
    else if (this.getLeft().isBorder()) {
      if (this.getRight().getLeft() != this) {
        return false;
      }
    }
    else if (this.getRight().getLeft() != this || this.getLeft().getRight() != this) {
      return false;
    }
    return true;
  }

  // returns an array list of each pixel below this pixel (including
  // this pixel) kicking off the accumulation helper with an empty list of pixels
  ArrayList<Pixel> constructPixelCol() {
    return this.constructPixelColHelp(new ArrayList<>());
  }

  // returns the pixel above this pixel
  public APixel getTop() {
    return this.top;
  }

  // returns the pixel to the right of this pixel
  public APixel getRight() {
    return this.right;
  }

  // returns the pixel below this pixel
  public APixel getBottom() {
    return this.bottom;
  }

  // returns the pixel to the left of this pixel
  public APixel getLeft() {
    return this.left;
  }

  // EFFECT: sets this pixel's target neighbor to refer to the given pixel, and
  // makes the given pixel refer to this one
  // 0 implies top, 1 implies right, 2 implies bottom, 3 implies left
  public void setNeighbor(Pixel p, int direction) {
    if (direction < 0 || direction > 3) {
      throw new RuntimeException("Direciton should be between 0 and 3!");
    }

    // 0 implies top, 1 implies right, 2 implies bottom, 3 implies left
    switch (direction) {
      case 0:
        this.top = p;
        break;
      case 1:
        this.right = p;
        break;
      case 2:
        this.bottom = p;
        break;
      case 3:
        this.left = p;
        break;
      default:
        // no other case applies
        break;
    }

    // ternary operator:
    // 0 (top) maps to 2 (bottom) on the other pixel
    // 1 -> 3
    // 3 (left) maps to 1 (right) on the other pixel
    // 2 -> 0
    p.setNeighborHelp(this, direction < 2 ? direction + 2 : direction - 2);
  }

  // EFFECT: sets this pixel's target neighbor to refer to the given pixel
  // 0 implies top, 1 implies right, 2 implies bottom, 3 implies left
  public void setNeighborHelp(Pixel p, int direction) {
    switch (direction) {
      case 0:
        this.top = p;
        break;
      case 1:
        this.right = p;
        break;
      case 2:
        this.bottom = p;
        break;
      case 3:
        this.left = p;
        break;
      default:
        // no other case applies
        break;
    }

  }

  // returns 1 + the row count of the pixel below this pixel
  public int rowCount() {
    return 1 + this.getBottom().rowCount();
  }

  // returns 1 + the col count of the pixel to the right of this pixel
  public int colCount() {
    return 1 + this.getRight().colCount();
  }

  // either returns this pixel if both row and col offset are 0, or returns the
  // pixel at getPixelAt(row - 1, col - 1)
  public Pixel getPixelAt(int rowOffset, int colOffset) {
    if (rowOffset == 0 && colOffset == 0) {
      return this;
    }
    else if (rowOffset == 0) {
      return this.getRight().getPixelAt(rowOffset, colOffset - 1);
    }
    else if (colOffset == 0) {
      return this.getBottom().getPixelAt(rowOffset - 1, colOffset);
    }
    else {
      return this.getBottom().getRight().getPixelAt(rowOffset - 1, colOffset - 1);
    }
  }

  // returns an array list of each pixel to the right of this pixel (including
  // this pixel) kicking off the accumulation helper with an empty list of pixels
  ArrayList<Pixel> constructPixelRow() {
    return this.constructPixelRowHelp(new ArrayList<>());
  }

  // EFFECT:
  // adds the current pixel to the list so far, and returns the result of applying
  // this action recursively to every pixel to the right of this pixel
  public ArrayList<Pixel> constructPixelRowHelp(ArrayList<Pixel> listSoFar) {
    listSoFar.add(this);
    return this.getRight().constructPixelRowHelp(listSoFar);
  }

  // EFFECT: sets this pixel's neighbor to the given APixel, where 0 implies top,
  // 1 implies right, 2 implies bottom, 3 implies left
  public void changeNeighbor(APixel newPixel, int direction) {
    // 0 implies top, 1 implies right, 2 implies bottom, 3 implies left
    switch (direction) {
      case 0:
        this.top = newPixel;
        break;
      case 1:
        this.right = newPixel;
        break;
      case 2:
        this.bottom = newPixel;
        break;
      case 3:
        this.left = newPixel;
        break;
      default:
        // no other case applies
        break;
    }
  }

  // returns that this is not a border pixel
  public boolean isBorder() {
    return false;
  }

  // returns either the maskedColor if it has been initialized or the pixel's
  // color
  public Color getColor() {
    return this.maskedColor ? Color.red : this.color;
  }

  // EFFECT:
  // adds the current pixel to the list so far, and returns the result of applying
  // this action recursively to every pixel below this pixel
  public ArrayList<Pixel> constructPixelColHelp(ArrayList<Pixel> listSoFar) {
    listSoFar.add(this);
    return this.getBottom().constructPixelColHelp(listSoFar);
  }

  // returns this pixel
  public Pixel asPixel() {
    return this;
  }

  // returns the gray scaled version of this pixel based on the max energy
  public Color getGrayScale(double maxEnergy) {
    int grayRatio = (int) (255 * (this.energy() / maxEnergy));
    return new Color(grayRatio, grayRatio, grayRatio);
  }

  // returns the energy of the pixel with highest energy in this row
  public double getMaximumEnergyRow(double currMax) {
    if (this.energy() > currMax) {
      return this.getRight().getMaximumEnergyRow(this.energy());
    }
    else {
      return this.getRight().getMaximumEnergyRow(currMax);
    }

  }

  // returns the energy of the pixel with highest energy in this grid
  public double getMaximumEnergyGrid(double currMax) {

    double currRowMax = this.getMaximumEnergyRow(currMax);
    if (currRowMax > currMax) {
      return this.getBottom().getMaximumEnergyGrid(currRowMax);
    }
    else {
      return this.getBottom().getMaximumEnergyGrid(currMax);
    }
  }

}

// represents a pixel that borders the image
class BorderPixel extends APixel {

  BorderPixel() {
    super(Color.black);
  }

  // a border pixel does not count as a row
  public int rowCount() {
    return 0;
  }

  // a border pixel does not count as a col
  public int colCount() {
    return 0;
  }

  // a border pixel represents the base case, cannot go from a base case to a
  // non-base case
  public APixel getTop() {
    return new BorderPixel();
  }

  // a border pixel represents the base case, cannot go from a base case to a
  // non-base case
  public APixel getRight() {
    return new BorderPixel();
  }

  // a border pixel represents the base case, cannot go from a base case to a
  // non-base case
  public APixel getBottom() {
    return new BorderPixel();
  }

  // a border pixel represents the base case, cannot go from a base case to a
  // non-base case
  public APixel getLeft() {
    return new BorderPixel();
  }

  // a border pixel indicates an out of bounds exception as too large of an offset
  // was passed in for either dimension
  public Pixel getPixelAt(int rowOffset, int colOffset) {
    throw new RuntimeException("Index out of bounds exception for getPixelAt!");
  }

  // indicates we have reached the end of the list, so return the listSoFar
  public ArrayList<Pixel> constructPixelRowHelp(ArrayList<Pixel> listSoFar) {
    return listSoFar;
  }

  // EFFECT: nothing
  // does nothing because a border pixel is only pointed at
  public void changeNeighbor(APixel newPixel, int direction) {
    return;
  }

  // returns that this is a border pixel
  public boolean isBorder() {
    return true;
  }

  // cannot call getColor on a border pixel
  public Color getColor() {
    throw new RuntimeException("Border pixel has no color!");
  }

  // indicates we have reached the end of the list, so return the listSoFar
  public ArrayList<Pixel> constructPixelColHelp(ArrayList<Pixel> listSoFar) {
    return listSoFar;
  }

  // we do not call asPixel on a border pixel
  public Pixel asPixel() {
    throw new RuntimeException("Cannot call asPixel on a border pixel!");
  }

  // returns the energy of the pixel with highest energy in this row
  public double getMaximumEnergyRow(double currMax) {
    return currMax;
  }

  // returns the energy of the pixel with highest energy in this grid
  public double getMaximumEnergyGrid(double currMax) {
    return currMax;
  }

  // a border pixel has no gray scale as it has no color
  public Color getGrayScale(double maxEnergy) {
    throw new RuntimeException("Border pixel has no energy!");
  }

}

class ExamplesIE {

  Pixel singlePixel;

  Grid grid;
  Grid gridCopy;
  Grid grid2;
  Grid grid2Copy;
  Grid grid2Remove1Seam;
  Grid grid3x3;
  Grid grid3x3Copy;

  ImageEditor ie;
  ImageEditor ieCopy;
  ImageEditor ie2;
  ImageEditor ie2Copy;

  FromFileImage balloons = new FromFileImage("./balloons.jpeg");
  FromFileImage tinyImg1 = new FromFileImage("./tinyImage1.png");
  FromFileImage tinyImg1Remove1Seam = new FromFileImage("./4x4_1.png");
  FromFileImage img3x3 = new FromFileImage("./img3x3.png");

  void testBigBang(Tester t) {
    // initialize conditions
    this.initConds();

    ie.bigBang(1000, 1000, 0.1);
  }

  // EFFECT: initializes the conditions for testing
  void initConds() {
    this.singlePixel = new Pixel(Color.blue);
    this.grid = new Grid(balloons);
    this.grid2Copy = new Grid(balloons);
    this.grid2 = new Grid(tinyImg1);
    this.grid2Copy = new Grid(tinyImg1);
    this.grid2Remove1Seam = new Grid(tinyImg1Remove1Seam);
    this.grid3x3 = new Grid(img3x3);
    this.grid3x3Copy = new Grid(img3x3);
    this.ie = new ImageEditor(grid);
    this.ie2 = new ImageEditor(grid2);
    this.ieCopy = new ImageEditor(gridCopy);
    this.ie2Copy = new ImageEditor(grid2Copy);
  }

  void testEnergiesAndBrightness(Tester t) {
    // initialize conditions
    this.initConds();

    t.checkExpect(new BorderPixel().energy(), 0.0);
    t.checkInexact(this.singlePixel.energy(), 0.0, 0.001);

    this.singlePixel = this.grid2.rootPixel;
    t.checkInexact(this.singlePixel.energy(), 2.182, 0.001);

    this.singlePixel = this.grid2.rootPixel.getPixelAt(1, 1);
    t.checkInexact(this.singlePixel.energy(), 0.410, 0.001);

    t.checkInexact(this.singlePixel.hEnergy(), -0.2667, 0.001);
    t.checkInexact(this.singlePixel.vEnergy(), 0.311, 0.001);
    t.checkInexact(this.singlePixel.brightness(), 0.557, 0.001);

    t.checkInexact(new BorderPixel().hEnergy(), 0.0, 0.001);
    t.checkInexact(new BorderPixel().vEnergy(), 0.0, 0.001);
    t.checkInexact(new BorderPixel().brightness(), 0.0, 0.001);
  }

  void testGridConstructor(Tester t) {
    // initialize conditions
    this.initConds();

    // check if grid has correct connections
    // top left pixel: does it refer to the correct edge pixels

    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 0).getTop().getRight(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 0).getTop(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 0).getTop().getLeft(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 0).getLeft(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 0).getBottom().getLeft(), new BorderPixel());

    // top left pixel: does it refer to the correct pixel neighbors? (check by
    // color)
    // create pixel colors at 0,1 and 0,0
    Color rightPixelColor = new Color(217, 14, 57);
    Color topLeftPixelColor = new Color(216, 13, 56);

    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 0).getRight().color, rightPixelColor);

    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 1).getLeft().color, topLeftPixelColor);

    // random edge pixel (not in corner): does it refer to the correct edge pixels?
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 69).getTop().getRight(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 69).getTop(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 69).getTop().getLeft(), new BorderPixel());

    // random edge pixel (not in corner): does it refer to the correct edge pixels?
    t.checkExpect(this.grid.rootPixel.getPixelAt(25, 799).getTop().getRight(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(25, 799).getRight(), new BorderPixel());
    t.checkExpect(this.grid.rootPixel.getPixelAt(25, 799).getBottom().getRight(),
        new BorderPixel());

    // random edge pixel (not in corner): does it refer to the correct pixel
    // neighbors?
    Color belowColor = new Color(132, 186, 246);
    Color belowRightColor = new Color(132, 186, 246);
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 69).getBottom().color, belowColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(0, 69).getBottom().getRight().color,
        belowRightColor);

    Color leftColor = new Color(2, 10, 95);
    Color topLeftColor = new Color(2, 10, 95);
    t.checkExpect(this.grid.rootPixel.getPixelAt(25, 799).getLeft().color, leftColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(25, 799).getTop().getLeft().color, topLeftColor);

    // random inner pixel: does it refer to the correct pixel neighbors?
    Color topColor = new Color(116, 154, 216);
    Color topRightColor = new Color(116, 154, 216);
    Color rightColor = new Color(119, 155, 217);
    Color bottomRightColor = new Color(119, 155, 217);
    Color bottomColor = new Color(119, 155, 217);
    Color bottomLeftColor = new Color(119, 155, 217);
    Color leftColor2 = new Color(119, 155, 217);
    Color topLeftColor2 = new Color(118, 154, 216);

    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getTop().color, topColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getTop().getRight().color, topRightColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getRight().color, rightColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getBottom().getRight().color,
        bottomRightColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getBottom().color, bottomColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getBottom().getLeft().color,
        bottomLeftColor);
    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getLeft().color, leftColor2);
    t.checkExpect(this.grid.rootPixel.getPixelAt(62, 420).getTop().getLeft().color, topLeftColor2);

    // test root pixel
    t.checkExpect(this.grid2.rootPixel, this.grid2.rootPixel.getPixelAt(0, 0));

    t.checkExpect(this.grid2.rootPixel.getTop(), new BorderPixel());
    t.checkExpect(this.grid2.rootPixel.getBottom(), this.grid2.rootPixel.getPixelAt(1, 0));
    t.checkExpect(this.grid2.rootPixel.getLeft(), new BorderPixel());
    t.checkExpect(this.grid2.rootPixel.getRight(), this.grid2.rootPixel.getPixelAt(0, 1));

    t.checkExpect(this.grid2.rootPixel.getBottom().getRight(),
        this.grid2.rootPixel.getPixelAt(1, 1));
    t.checkExpect(this.grid2.rootPixel.getBottom().getLeft(), new BorderPixel());
    t.checkExpect(this.grid2.rootPixel.getTop().getRight(), new BorderPixel());
    t.checkExpect(this.grid2.rootPixel.getTop().getLeft(), new BorderPixel());

    t.checkExpect(this.grid2.rootPixel.getBottom().getRight().getBottom().getRight().getBottom(),
        this.grid2.rootPixel.getPixelAt(3, 2));
  }

  void testSetNeighbor(Tester t) {
    // initialize conditions
    this.initConds();

    // make two pixels
    Pixel pixelAbove = new Pixel(Color.blue);
    Pixel pixelBelow = new Pixel(Color.red);

    // make sure they are not connected currently
    t.checkExpect(pixelAbove.bottom, new BorderPixel());
    t.checkExpect(pixelBelow.top, new BorderPixel());

    // set neighbors
    pixelAbove.setNeighbor(pixelBelow, 2);

    // make sure they are connected
    t.checkExpect(pixelAbove.bottom, pixelBelow);
    t.checkExpect(pixelBelow.top, pixelAbove);

    // other side of ternary operator:
    // make two pixels
    // initialize conditions
    this.initConds();
    pixelAbove = new Pixel(Color.blue);
    pixelBelow = new Pixel(Color.red);

    // make sure they are not connected currently
    t.checkExpect(pixelAbove.bottom, new BorderPixel());
    t.checkExpect(pixelBelow.top, new BorderPixel());

    // set neighbors
    pixelBelow.setNeighbor(pixelAbove, 0);

    // make sure they are connected
    t.checkExpect(pixelAbove.bottom, pixelBelow);
    t.checkExpect(pixelBelow.top, pixelAbove);

    // exception testing
    t.checkException(new RuntimeException("Direciton should be between 0 and 3!"), pixelBelow,
        "setNeighbor", pixelAbove, -1);
    t.checkException(new RuntimeException("Direciton should be between 0 and 3!"), pixelBelow,
        "setNeighbor", pixelAbove, 4);

  }

  void testGetPixelAt(Tester t) {
    // initialize conditions
    this.initConds();

    // test get pixel for same pixel
    t.checkExpect(this.grid2.rootPixel.getPixelAt(0, 0), this.grid2.rootPixel);

    // test get pixel on edges
    t.checkExpect(this.grid2.rootPixel.getPixelAt(1, 0), this.grid2.rootPixel.getBottom());
    t.checkExpect(this.grid2.rootPixel.getPixelAt(0, 1), this.grid2.rootPixel.getRight());

    // test pixel not on edge
    t.checkExpect(this.grid2.rootPixel.getPixelAt(1, 1),
        this.grid2.rootPixel.getBottom().getRight());

    // test asymmetric, not on edge, input
    t.checkExpect(this.grid2.rootPixel.getPixelAt(3, 2),
        this.grid2.rootPixel.getBottom().getBottom().getBottom().getRight().getRight());

    // test out of bounds
    t.checkException(new RuntimeException("Index out of bounds exception for getPixelAt!"),
        this.grid2.rootPixel, "getPixelAt", 3, 4);
  }

  void testSeamFinder(Tester t) {
    // initialize conditions
    this.initConds();

    // initialize weights, and the result seam
    double weight1 = this.grid2.rootPixel.getPixelAt(0, 2).energy();
    double weight2 = this.grid2.rootPixel.getPixelAt(1, 1).energy() + weight1;
    double weight3 = this.grid2.rootPixel.getPixelAt(2, 1).energy() + weight2;
    double weight4 = this.grid2.rootPixel.getPixelAt(3, 0).energy() + weight3;

    SeamInfo resultSeam = new SeamInfo(this.grid2.rootPixel.getPixelAt(3, 0), weight4,
        new SeamInfo(this.grid2.rootPixel.getPixelAt(2, 1), weight3,
            new SeamInfo(this.grid2.rootPixel.getPixelAt(1, 1), weight2,
                new SeamInfo(this.grid2.rootPixel.getPixelAt(0, 2), weight1, null))));

    // make sure finding lowest seam returns correct result
    t.checkExpect(this.grid2.findLowestSeamVertical(), resultSeam);

    // initialize weights, and the result seam
    weight1 = this.grid2.rootPixel.getPixelAt(2, 0).energy();
    weight2 = this.grid2.rootPixel.getPixelAt(1, 1).energy() + weight1;
    weight3 = this.grid2.rootPixel.getPixelAt(1, 2).energy() + weight2;
    weight4 = this.grid2.rootPixel.getPixelAt(0, 3).energy() + weight3;

    resultSeam = new SeamInfo(this.grid2.rootPixel.getPixelAt(0, 3), weight4,
        new SeamInfo(this.grid2.rootPixel.getPixelAt(1, 2), weight3,
            new SeamInfo(this.grid2.rootPixel.getPixelAt(1, 1), weight2,
                new SeamInfo(this.grid2.rootPixel.getPixelAt(2, 0), weight1, null))));

    // make sure finding lowest seam returns correct result
    t.checkExpect(this.grid2.findLowestSeamHorizontal(), resultSeam);

  }

  void testMakeScene(Tester t) {
    // initialize conditions
    this.initConds();

    WorldScene w = new WorldScene(1000, 1000);
    w.placeImageXY(this.grid.makeImage(false), 500, 500);

    t.checkExpect(this.ie.makeScene(), w);

    // initialize conditions
    this.initConds();

    this.ie.grayScaleOffEnergy = true;

    w = new WorldScene(1000, 1000);
    w.placeImageXY(this.grid.makeImage(true), 500, 500);

    t.checkExpect(this.ie.makeScene(), w);
  }

  void testMakeImage(Tester t) {
    // initialize conditions
    this.initConds();

    // mutate img
    ComputedPixelImage img = new ComputedPixelImage(4, 4);
    this.grid2.makeImageRow(this.grid2.rootPixel, img, 4, 0, true, this.grid2.getMaxEnergy());
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom(), img, 4, 1, true,
        this.grid2.getMaxEnergy());
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom().getBottom(), img, 4, 2, true,
        this.grid2.getMaxEnergy());
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom().getBottom().getBottom(), img, 4, 3,
        true, this.grid2.getMaxEnergy());

    t.checkExpect(this.grid2.makeImage(true), img);

    // initialize conditions
    this.initConds();

    // mutate img
    img = new ComputedPixelImage(4, 4);
    this.grid2.makeImageRow(this.grid2.rootPixel, img, 4, 0, false, this.grid2.getMaxEnergy());
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom(), img, 4, 1, false,
        this.grid2.getMaxEnergy());
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom().getBottom(), img, 4, 2, false,
        this.grid2.getMaxEnergy());
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom().getBottom().getBottom(), img, 4, 3,
        false, this.grid2.getMaxEnergy());

    t.checkExpect(this.grid2.makeImage(false), img);
  }

  void testMakeImageRow(Tester t) {
    // initialize conditions
    this.initConds();

    ComputedPixelImage img = new ComputedPixelImage(4, 4);

    // no grayscale case

    // check if there is no pixels in there so far
    t.checkExpect(img.getPixel(0, 1), new Color(0, 0, 0, 0));
    t.checkExpect(img.getPixel(1, 1), new Color(0, 0, 0, 0));
    t.checkExpect(img.getPixel(2, 1), new Color(0, 0, 0, 0));
    t.checkExpect(img.getPixel(3, 1), new Color(0, 0, 0, 0));

    // update them
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom(), img, 4, 1, false, 0);

    // check if the pixels now refer to grid2 pixels
    t.checkExpect(img.getPixel(0, 1), new Color(161, 23, 140));
    t.checkExpect(img.getPixel(1, 1), new Color(38, 157, 231));
    t.checkExpect(img.getPixel(2, 1), new Color(38, 157, 231));
    t.checkExpect(img.getPixel(3, 1), new Color(38, 231, 38));

    // gray scale case
    // initialize conditions
    this.initConds();

    img = new ComputedPixelImage(4, 4);

    // check if there is no pixels in there so far
    t.checkExpect(img.getPixel(0, 1), new Color(0, 0, 0, 0));
    t.checkExpect(img.getPixel(1, 1), new Color(0, 0, 0, 0));
    t.checkExpect(img.getPixel(2, 1), new Color(0, 0, 0, 0));
    t.checkExpect(img.getPixel(3, 1), new Color(0, 0, 0, 0));

    // update them
    this.grid2.makeImageRow(this.grid2.rootPixel.getBottom(), img, 4, 1, true, 5);

    // check if the pixels now refer to grid2 pixels
    t.checkExpect(img.getPixel(0, 1), new Color(123, 123, 123));
    t.checkExpect(img.getPixel(1, 1), new Color(20, 20, 20));
    t.checkExpect(img.getPixel(2, 1), new Color(58, 58, 58));
    t.checkExpect(img.getPixel(3, 1), new Color(96, 96, 96));
  }

  void testOnTick(Tester t) {
    // initialize conditions
    this.initConds();

    // make sure ie refers to ie2 copy
    t.checkExpect(this.ie2, this.ie2Copy);

    // test paused behavior:
    this.ie2.paused = true;
    this.ie2Copy.paused = true;

    // tick
    this.ie2.onTick();

    // make sure ie refers to ie2 copy
    t.checkExpect(this.ie2, this.ie2Copy);

    // test when img too small
    this.initConds();

    // make img small
    this.grid2.removeSeamHorizontal(false);
    this.grid2.removeSeamHorizontal(false);

    this.grid2Copy.removeSeamHorizontal(false);
    this.grid2Copy.removeSeamHorizontal(false);

    // tick
    this.ie2.onTick();

    // make sure ontick didnt change anything
    t.checkExpect(this.ie2, this.ie2Copy);

    this.initConds();

    // make editor start reversing
    this.ie2.inflating = true;
    this.ie2Copy.inflating = true;

    // no history of seams, should do nothing when inflating
    this.ie2.onTick();

    t.checkExpect(this.ie2, this.ie2Copy);

    // remove seams and add to history
    this.ie2.seamHistory.add(this.grid2.findLowestSeamHorizontal());
    this.grid2.removeSeamHorizontal(false);

    // tick, should reinsert the removed seams
    this.ie2.onTick();

    t.checkExpect(this.ie2, this.ie2Copy);

    this.initConds();

    // make editor start reversing
    this.ie2.inflating = true;
    this.ie2Copy.inflating = true;

    // no history of seams, should do nothing when inflating
    this.ie2.onTick();

    t.checkExpect(this.ie2, this.ie2Copy);

    // remove seams and add to history
    this.ie2.seamHistory.add(this.grid2.findLowestSeamHorizontal());
    this.grid2.removeSeamHorizontal(false);

    this.ie2.seamHistory.add(this.grid2.findLowestSeamVertical());
    this.grid2.removeSeamVertical(false);

    // tick, should reinsert the removed seams
    this.ie2.onTick();
    this.ie2.onTick();

    t.checkExpect(this.ie2, this.ie2Copy);

    this.initConds();

    // test drawing redline
    this.ie2.onTick();

    // update copy to match

    if (this.ie2Copy.rand.nextInt(2) == 1) {
      this.ie2Copy.removeVertical = true;
      this.ie2Copy.grid.removeSeamVertical(true);
    }
    else {
      this.ie2Copy.removeVertical = false;
      this.ie2Copy.grid.removeSeamHorizontal(true);
    }

    this.ie2Copy.removeSeam = true;

    t.checkExpect(this.ie2, this.ie2Copy);

    this.initConds();

    // test remove horizontal override
    this.ie2.removeHorizontalOverride = true;
    this.ie2.removeSeam = false;

    // tick
    this.ie2.onTick();

    // update copy to match
    this.ie2Copy.seamHistory.add(this.ie2Copy.grid.findLowestSeamHorizontal());
    this.ie2Copy.grid.removeSeamHorizontal(false);

    this.ie2Copy.removeVerticalOverride = false;
    this.ie2Copy.removeHorizontalOverride = false;

    // check if ontick behaves as expected
    t.checkExpect(this.ie2, this.ie2Copy);

    this.initConds();

    // test remove vertical override
    this.ie2.removeVerticalOverride = true;
    this.ie2.removeSeam = false;

    // tick
    this.ie2.onTick();

    // update copy to match
    this.ie2Copy.seamHistory.add(this.ie2Copy.grid.findLowestSeamVertical());
    this.ie2Copy.grid.removeSeamVertical(false);

    this.ie2Copy.removeVerticalOverride = false;
    this.ie2Copy.removeHorizontalOverride = false;

    // check if ontick behaves as expected
    t.checkExpect(this.ie2, this.ie2Copy);

    this.initConds();

    // test remove vertical normal behavior
    this.ie2.removeSeam = true;
    this.ie2.removeVertical = true;

    // tick
    this.ie2.onTick();

    // update copy to match
    this.ie2Copy.seamHistory.add(this.ie2Copy.grid.findLowestSeamVertical());
    this.ie2Copy.grid.removeSeamVertical(false);

    // check if ontick behaves as expected
    t.checkExpect(this.ie2, this.ie2Copy);

  }

  void testRemoveInRow(Tester t) {
    // initialize conditions
    this.initConds();

    // check that grid refers to original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // get the lowest seam
    SeamInfo seam = this.grid2.findLowestSeamVertical();

    // remove the row based on the seam
    seam.removeInRow(true);

    // check that the color is red
    t.checkExpect(this.grid2.rootPixel.getPixelAt(3, 0).maskedColor, true);

    // check if also returns null
    t.checkExpect(seam.removeInRow(true), null);

    // initialize conditions
    this.initConds();
    seam = this.grid2.findLowestSeamVertical();

    // remove the row based on the seam
    seam.removeInRow(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(3, 0).color,
        this.grid2Copy.rootPixel.getPixelAt(3, 1).color);

    this.initConds();
    seam = this.grid2.findLowestSeamVertical();

    // check if also returns null
    t.checkExpect(seam.removeInRow(false), null);

    // initialize conditions
    this.initConds();
    this.grid2.removeSeamVertical(false);
    seam = this.grid2.findLowestSeamVertical();

    // remove the row based on the seam
    seam.removeInRow(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(2, 1).color, new Color(38, 157, 231));

    seam = this.grid2.findLowestSeamVertical();

    // remove the row based on the seam
    seam.removeInRow(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(1, 1).color, new Color(38, 157, 231));

    this.initConds();

    // remove two seams in vertical direction
    this.grid2.removeSeamVertical(false);
    this.grid2.removeSeamVertical(false);
    seam = this.grid2.findLowestSeamVertical();
    // remove the row based on the seam
    seam.removeInRow(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(0, 1).color, new Color(38, 231, 38));

    // check if can mutate the rootpixel's reference
    Pixel rootPixel = new Pixel(new Color(0, 0, 1));
    Pixel rightPixel = new Pixel(new Color(1, 0, 0));
    rootPixel.setNeighbor(rightPixel, 1);
    t.checkExpect(new SeamInfo(rootPixel, 1, null).removeInRow(false), rightPixel);

    // test well formedness exceptions
    Pixel badPixel1 = new Pixel(new Color(0, 0, 1));
    Pixel badPixel2 = new Pixel(new Color(1, 0, 0));

    badPixel1.setNeighbor(badPixel2, 1);
    badPixel2.changeNeighbor(new BorderPixel(), 3);
    SeamInfo seamError = new SeamInfo(badPixel1, 1, null);

    t.checkException(new RuntimeException("Row not well formed before removal!"), seamError,
        "removeInRow", false);
  }

  void testRemoveInCol(Tester t) {
    // initialize conditions
    this.initConds();

    // check that grid refers to original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // get the lowest seam
    SeamInfo seam = this.grid2.findLowestSeamHorizontal();

    // remove the row based on the seam
    seam.removeInCol(true);

    // check that the color is red
    t.checkExpect(this.grid2.rootPixel.getPixelAt(0, 3).maskedColor, true);

    // check if also returns null
    t.checkExpect(seam.removeInCol(true), null);

    // initialize conditions
    this.initConds();
    seam = this.grid2.findLowestSeamHorizontal();

    // remove the row based on the seam
    seam.removeInCol(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(3, 0).color,
        this.grid2Copy.rootPixel.getPixelAt(2, 1).color);

    this.initConds();
    seam = this.grid2.findLowestSeamHorizontal();

    // check if also returns null
    t.checkExpect(seam.removeInCol(false), null);

    // initialize conditions
    this.initConds();
    this.grid2.removeSeamHorizontal(false);
    seam = this.grid2.findLowestSeamHorizontal();

    // remove the row based on the seam
    seam.removeInCol(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(2, 1).color, new Color(161, 23, 140));

    seam = this.grid2.findLowestSeamHorizontal();

    // remove the row based on the seam
    seam.removeInCol(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(1, 1).color, new Color(38, 231, 38));

    this.initConds();

    // remove two seams in horziontal direction
    this.grid2.removeSeamHorizontal(false);
    this.grid2.removeSeamHorizontal(false);
    seam = this.grid2.findLowestSeamHorizontal();
    // remove the row based on the seam
    seam.removeInCol(false);

    // check if the pixel was properly removed
    t.checkExpect(this.grid2.rootPixel.getPixelAt(0, 1).color, new Color(38, 157, 231));

    // check if can mutate the rootpixel's reference
    Pixel rootPixel = new Pixel(new Color(0, 0, 1));
    Pixel bottomPixel = new Pixel(new Color(1, 0, 0));
    rootPixel.setNeighbor(bottomPixel, 2);
    t.checkExpect(new SeamInfo(rootPixel, 1, null).removeInCol(false), bottomPixel);

    // test well formedness exceptions
    Pixel badPixel1 = new Pixel(new Color(0, 0, 1));
    Pixel badPixel2 = new Pixel(new Color(1, 0, 0));

    badPixel1.setNeighbor(badPixel2, 2);
    badPixel2.changeNeighbor(new BorderPixel(), 0);
    SeamInfo seamError = new SeamInfo(badPixel1, 1, null);

    t.checkException(new RuntimeException("Col not well formed before removal!"), seamError,
        "removeInCol", false);
  }

  void testRemoveSeam(Tester t) {
    // initialize conditions
    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // remove the seam
    this.grid2.removeSeamVertical(false);

    // update grid2Copy to match
    SeamInfo lowestSeam = this.grid2Copy.findLowestSeamVertical();
    this.grid2Copy.removeSeamVerticalHelp(lowestSeam, false);

    // grid 2 should still refer to its copy
    t.checkExpect(this.grid2, this.grid2Copy);

    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // remove the seam
    this.grid2.removeSeamHorizontal(false);

    // update grid2Copy to match
    lowestSeam = this.grid2Copy.findLowestSeamHorizontal();
    this.grid2Copy.removeSeamHorizontalHelp(lowestSeam, false);

    // grid 2 should still refer to its copy
    t.checkExpect(this.grid2, this.grid2Copy);

    // initialize conditions
    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // remove the seam
    this.grid2.removeSeamVertical(true);

    // update grid2Copy to match
    lowestSeam = this.grid2Copy.findLowestSeamVertical();
    this.grid2Copy.removeSeamVerticalHelp(lowestSeam, true);

    // grid 2 should still refer to its copy
    t.checkExpect(this.grid2, this.grid2Copy);

    // initialize conditions
    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // remove the seam
    this.grid2.removeSeamHorizontal(true);

    // update grid2Copy to match
    lowestSeam = this.grid2Copy.findLowestSeamHorizontal();
    this.grid2Copy.removeSeamHorizontalHelp(lowestSeam, true);

    // grid 2 should still refer to its copy
    t.checkExpect(this.grid2, this.grid2Copy);
  }

  void testRemoveSeamHelp(Tester t) {
    // initialize conditions
    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    this.grid2.removeSeamVerticalHelp(null, false);

    // grid 2 should still refer to its copy
    t.checkExpect(this.grid2, this.grid2Copy);

    // initialize conditions
    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    this.grid2.removeSeamHorizontalHelp(null, false);

    // grid 2 should still refer to its copy
    t.checkExpect(this.grid2, this.grid2Copy);

    // initialize conditions
    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // remove the seam
    this.grid2.removeSeamVerticalHelp(this.grid2.findLowestSeamVertical(), true);

    // update copy to match
    SeamInfo seam = this.grid2Copy.findLowestSeamVertical();
    seam.removeInRow(true);
    this.grid2Copy.removeSeamVerticalHelp(seam.cameFrom, true);

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // initialize conditions
    this.initConds();

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    // remove the seam
    this.grid2.removeSeamHorizontalHelp(this.grid2.findLowestSeamHorizontal(), true);

    // update copy to match
    seam = this.grid2Copy.findLowestSeamHorizontal();
    seam.removeInCol(true);
    this.grid2Copy.removeSeamHorizontalHelp(seam.cameFrom, true);

    // make sure grid refers to its original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);
  }

  void testFindMinSeamEnergy(Tester t) {
    // initialize conditions
    this.initConds();

    ArrayList<SeamInfo> seams = new ArrayList<>();
    seams.add(new SeamInfo(null, 10, null));
    seams.add(new SeamInfo(null, 1, null));
    seams.add(new SeamInfo(null, 5, null));
    t.checkExpect(this.grid2.findMinSeamEnergy(seams), seams.get(1));
  }

  void testGetColor(Tester t) {
    // initialize conditions
    this.initConds();

    t.checkExpect(this.grid2.rootPixel.getColor(), this.grid2.rootPixel.color);
    t.checkException(new RuntimeException("Border pixel has no color!"), new BorderPixel(),
        "getColor");

    this.grid2.rootPixel.maskedColor = true;
    t.checkExpect(this.grid2.rootPixel.getColor(), Color.red);

  }

  void testIsBorder(Tester t) {
    // initialize conditions
    this.initConds();

    t.checkExpect(this.grid2.rootPixel.isBorder(), false);
    t.checkExpect(this.grid2.rootPixel.getLeft().isBorder(), true);
  }

  void testRowAndColCount(Tester t) {
    // initialize conditions
    this.initConds();

    t.checkExpect(this.grid.rootPixel.rowCount(), 343);
    t.checkExpect(this.grid.rootPixel.colCount(), 800);

  }

  void testConstructPixelRow(Tester t) {
    // initialize conditions
    this.initConds();

    ArrayList<Pixel> pixels = new ArrayList<>();
    pixels.add(this.grid2.rootPixel);
    pixels.add((Pixel) this.grid2.rootPixel.getRight());
    pixels.add((Pixel) this.grid2.rootPixel.getRight().getRight());
    pixels.add((Pixel) this.grid2.rootPixel.getRight().getRight().getRight());

    t.checkExpect(this.grid2.rootPixel.constructPixelRow(), pixels);
    t.checkExpect(this.grid2.rootPixel.constructPixelRowHelp(new ArrayList<>()), pixels);
  }

  void testConstructPixelCol(Tester t) {
    // initialize conditions
    this.initConds();

    ArrayList<Pixel> pixels = new ArrayList<>();
    pixels.add(this.grid2.rootPixel);
    pixels.add((Pixel) this.grid2.rootPixel.getBottom());
    pixels.add((Pixel) this.grid2.rootPixel.getBottom().getBottom());
    pixels.add((Pixel) this.grid2.rootPixel.getBottom().getBottom().getBottom());

    t.checkExpect(this.grid2.rootPixel.constructPixelCol(), pixels);
    t.checkExpect(this.grid2.rootPixel.constructPixelColHelp(new ArrayList<>()), pixels);
  }

  void testGetPixel(Tester t) {
    // initialize conditions
    this.initConds();

    t.checkExpect(this.grid2.rootPixel.getTop(), new BorderPixel());
    t.checkExpect(this.grid2.rootPixel.getRight(), this.grid2.rootPixel.right);
    t.checkExpect(this.grid2.rootPixel.getLeft(), new BorderPixel());
    t.checkExpect(this.grid2.rootPixel.getBottom(), this.grid2.rootPixel.bottom);
  }

  void testChangeNeighbor(Tester t) {
    // initialize conditions
    this.initConds();

    // check that grid refers to original state (copy)
    t.checkExpect(this.grid2, this.grid2Copy);

    this.grid2.rootPixel.changeNeighbor(new BorderPixel(), 0);

    // check that grid didnt change
    t.checkExpect(this.grid2, this.grid2Copy);

    new BorderPixel().changeNeighbor(this.grid2.rootPixel, 0);

    // check that grid didnt change
    t.checkExpect(this.grid2, this.grid2Copy);

    // check top reference
    t.checkExpect(this.grid2.rootPixel.getBottom().getTop(), this.grid2.rootPixel);

    // update top reference
    this.grid2.rootPixel.getBottom().changeNeighbor(this.grid2.rootPixel.getRight(), 0);

    // check that grid refers to new pixel
    t.checkExpect(this.grid2.rootPixel.getBottom().getTop(), this.grid2.rootPixel.getRight());

  }

  boolean testGetMaximumRow(Tester t) {
    this.initConds();
    return t.checkInexact(this.grid.rootPixel.getMaximumEnergyRow(this.grid.rootPixel.energy()),
        3.408, 0.001)
        && t.checkInexact(
            this.grid.rootPixel.getBottom().getMaximumEnergyRow(this.grid.rootPixel.energy()),
            1.985, 0.001);

  }

  boolean testGetMax(Tester t) {
    this.initConds();
    return t.checkInexact(this.grid.getMaxEnergy(), 3.828, 0.01);
  }

  void testReinsertSeam(Tester t) {
    this.initConds();

    // store the removed seams
    SeamInfo firstRemovedSeam = this.grid2.findLowestSeamVertical();
    SeamInfo secondRemovedSeam = this.grid2.findLowestSeamHorizontal();

    // remove one seam
    this.grid2.removeSeamVertical(false);

    // confirm a removal
    t.checkExpect(this.grid2, this.grid2Remove1Seam);

    // reinsert seams
    this.grid2.reinsertSeam(firstRemovedSeam);

    // check no change
    t.checkExpect(this.grid2, this.grid2Copy);

    this.initConds();

    // reinsert removed seams
    this.grid2.reinsertSeam(secondRemovedSeam);
    this.grid2.reinsertSeam(firstRemovedSeam);

    // check no change
    t.checkExpect(this.grid2, this.grid2Copy);

    this.initConds();

    // should do nothing
    this.grid2.reinsertSeam(null);

    // check if seam still refers to copy
    t.checkExpect(this.grid2, this.grid2Copy);

    SeamInfo removedSeam = this.grid2.findLowestSeamHorizontal();

    // remove horizontal seam
    this.grid2.removeSeamHorizontal(false);

    // reinsert
    this.grid2.reinsertSeam(removedSeam);

    // check no change
    t.checkExpect(this.grid2, this.grid2Copy);
  }

  void testReinsertSeamHelp(Tester t) {
    this.initConds();

    // remove first hor seam
    SeamInfo firstRemovedSeam = this.grid2.findLowestSeamHorizontal();
    this.grid2.removeSeamHorizontal(false);

    // reinsert the seam
    firstRemovedSeam.reinsertSeamHelp();

    // make sure all pixels are in the right spots
    t.checkExpect(firstRemovedSeam.pixel.color, new Color(38, 231, 38));
    t.checkExpect(firstRemovedSeam.cameFrom.pixel.color, new Color(38, 157, 231));
    t.checkExpect(firstRemovedSeam.cameFrom.cameFrom.pixel.color, new Color(38, 157, 231));
    t.checkExpect(firstRemovedSeam.cameFrom.cameFrom.cameFrom.pixel.color, new Color(0, 0, 0));

    // make sure returns null when root pixel not modified
    t.checkExpect(firstRemovedSeam.reinsertSeamHelp(), null);

    Pixel rootPixel = new Pixel(new Color(0, 1, 0));

    // make sure throws exception when not well formed
    SeamInfo rootPixelReinsert = new SeamInfo(rootPixel, 1, null);
    t.checkException(new RuntimeException("Row or col not well formed after reinsertion!"),
        rootPixelReinsert, "reinsertSeamHelp");

    // make sure changes node when a rootpixel is reinserted
    Pixel oldRootPixel = new Pixel(new Color(1, 0, 0));
    Pixel aboveOldRootPixel = new Pixel(new Color(1, 2, 3));
    Pixel leftOldRootPixel = new Pixel(new Color(1, 2, 4));
    Pixel rightOldRootPixel = new Pixel(new Color(1, 2, 3));
    Pixel bottomOldRootPixel = new Pixel(new Color(1, 2, 4));

    rootPixel.setNeighbor(leftOldRootPixel, 2);
    rootPixel.setNeighbor(aboveOldRootPixel, 1);

    oldRootPixel.setNeighbor(leftOldRootPixel, 3);
    oldRootPixel.setNeighbor(aboveOldRootPixel, 0);
    oldRootPixel.setNeighbor(rightOldRootPixel, 1);
    oldRootPixel.setNeighbor(bottomOldRootPixel, 2);

    SeamInfo rootPixelReinsert2 = new SeamInfo(rootPixel, 1, new SeamInfo(oldRootPixel, 2, null));
    t.checkExpect(rootPixelReinsert2.reinsertSeamHelp(), rootPixel);

  }

  boolean testGetGridMaxEnergy(Tester t) {
    this.initConds();
    return t.checkInexact(this.grid.getMaxEnergy(), 3.828, 0.01);
  }

  void testGetGrayScalePixelColor(Tester t) {
    this.initConds();
    double maxEnergy = this.grid.getMaxEnergy();
    t.checkExpect(this.grid.rootPixel.getGrayScale(maxEnergy), new Color(106, 106, 106));
    t.checkExpect(this.grid.rootPixel.getRight().getGrayScale(maxEnergy), new Color(100, 100, 100));
    t.checkExpect(this.grid.rootPixel.getBottom().getGrayScale(maxEnergy),
        new Color(100, 100, 100));
    t.checkExpect(this.grid.rootPixel.getRight().getBottom().getGrayScale(maxEnergy),
        new Color(0, 0, 0));
    t.checkExpect(this.grid.rootPixel.getBottom().getRight().getGrayScale(maxEnergy),
        new Color(0, 0, 0));
    t.checkException(new RuntimeException("Border pixel has no energy!"), new BorderPixel(),
        "getGrayScale", 1.0);

  }

  void testOnKeyEvent(Tester t) {
    // initialize conditions
    this.initConds();

    // make sure that grid refers to its copy
    t.checkExpect(this.ie2, this.ie2Copy);

    // test that when the img size is too small (only r will be processed)
    this.grid2.removeSeamHorizontal(false);
    this.grid2.removeSeamHorizontal(false);

    this.grid2Copy.removeSeamHorizontal(false);
    this.grid2Copy.removeSeamHorizontal(false);

    // press a (random key)
    this.ie2.onKeyEvent("a");

    // make sure grid didnt change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press h (remove hor)
    this.ie2.onKeyEvent("h");

    // make sure grid didnt change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press v (remove ver)
    this.ie2.onKeyEvent("v");

    // make sure grid didnt change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press g (toggle grayscale)
    this.ie2.onKeyEvent("g");

    // make sure grid didnt change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press space (toggle pause)
    this.ie2.onKeyEvent(" ");

    // make sure grid didnt change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press r (toggle reverse)
    this.ie2.onKeyEvent("r");

    // update copy
    this.ie2Copy.inflating = !this.ie2Copy.inflating;

    // confirm change
    t.checkExpect(this.ie2, this.ie2Copy);

    // initialize conds
    this.initConds();

    // press space (toggle pause)
    this.ie2.onKeyEvent(" ");

    // update copy
    this.ie2Copy.paused = !this.ie2Copy.paused;

    // confirm change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press v (queue vertical remove)
    this.ie2.onKeyEvent("v");

    // update copy
    this.ie2Copy.removeVerticalOverride = true;
    this.ie2Copy.removeHorizontalOverride = false;

    // confirm change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press h (queue horizontal remove)
    this.ie2.onKeyEvent("h");

    // update copy
    this.ie2Copy.removeVerticalOverride = false;
    this.ie2Copy.removeHorizontalOverride = true;

    // confirm change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press g (toggle grayscale)
    this.ie2.onKeyEvent("g");

    // update copy
    this.ie2.grayScaleOffEnergy = !this.ie2.grayScaleOffEnergy;

    // confirm change
    t.checkExpect(this.ie2, this.ie2Copy);

    // press g (toggle reversal)
    this.ie2.onKeyEvent("r");

    // update copy
    this.ie2.inflating = !this.ie2.inflating;

    // confirm change
    t.checkExpect(this.ie2, this.ie2Copy);
  }

  void testGetMaxEnergy(Tester t) {
    this.initConds();

    t.checkInexact(this.grid2.getMaxEnergy(), 2.43, 0.01);
    t.checkInexact(this.grid.getMaxEnergy(), 3.83, 0.01);
    t.checkInexact(this.grid2Remove1Seam.getMaxEnergy(), 2.49, 0.01);
  }

  void testFixPixels(Tester t) {
    this.initConds();

    ArrayList<ArrayList<Pixel>> list2d = new ArrayList<>();

    // set up a grid
    Pixel pixel00 = new Pixel(new Color(143, 192, 235));
    Pixel pixel01 = new Pixel(new Color(221, 143, 235));
    Pixel pixel02 = new Pixel(new Color(221, 143, 235));

    Pixel pixel10 = new Pixel(new Color(143, 192, 235));
    Pixel pixel11 = new Pixel(new Color(143, 192, 235));
    Pixel pixel12 = new Pixel(new Color(221, 143, 235));

    Pixel pixel20 = new Pixel(new Color(221, 143, 235));
    Pixel pixel21 = new Pixel(new Color(143, 192, 235));
    Pixel pixel22 = new Pixel(new Color(143, 192, 235));

    list2d.add(new ArrayList<>());
    list2d.add(new ArrayList<>());
    list2d.add(new ArrayList<>());

    list2d.get(0).add(pixel00);
    list2d.get(0).add(pixel01);
    list2d.get(0).add(pixel02);

    list2d.get(1).add(pixel10);
    list2d.get(1).add(pixel11);
    list2d.get(1).add(pixel12);

    list2d.get(2).add(pixel20);
    list2d.get(2).add(pixel21);
    list2d.get(2).add(pixel22);

    // test corners, non-corner edges, non edge or corner case

    // fix pixel connections at 0,0 and 0,1...
    this.grid3x3.connectPixels(img3x3, list2d, 0, 0, pixel00);
    t.checkExpect(pixel00.getRight().getLeft(), pixel00);
    t.checkExpect(pixel00.getBottom().getTop(), pixel00);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 0, 1, pixel01);
    t.checkExpect(pixel01.getRight().getLeft(), pixel01);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 0, 2, pixel02);
    t.checkExpect(pixel02.getLeft().getRight(), pixel02);
    t.checkExpect(pixel02.getBottom().getTop(), pixel02);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 1, 0, pixel10);
    t.checkExpect(pixel10.getBottom().getTop(), pixel10);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 1, 1, pixel11);
    t.checkExpect(pixel11.getBottom().getTop(), pixel11);
    t.checkExpect(pixel11.getRight().getLeft(), pixel11);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 1, 2, pixel12);
    t.checkExpect(pixel12.getBottom().getTop(), pixel12);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 2, 0, pixel20);
    t.checkExpect(pixel20.getTop().getBottom(), pixel20);
    t.checkExpect(pixel20.getRight().getLeft(), pixel20);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 2, 1, pixel21);
    t.checkExpect(pixel21.getRight().getLeft(), pixel21);

    this.initConds();

    this.grid3x3.connectPixels(img3x3, list2d, 2, 2, pixel22);
    t.checkExpect(pixel22.getTop().getBottom(), pixel22);
    t.checkExpect(pixel22.getLeft().getRight(), pixel22);

  }

  void testAsPixel(Tester t) {
    this.initConds();

    t.checkExpect(new Pixel(Color.black).asPixel(), new Pixel(Color.black));
    t.checkException(new RuntimeException("Cannot call asPixel on a border pixel!"),
        new BorderPixel(), "asPixel");
  }

  void testGetMaximumEnergyGridAndRow(Tester t) {
    this.initConds();

    t.checkInexact(this.grid2.rootPixel.getMaximumEnergyGrid(-1), 2.43, 0.01);
    t.checkInexact(this.grid.rootPixel.getMaximumEnergyGrid(-1), 3.83, 0.01);

    t.checkInexact(this.grid2.rootPixel.getMaximumEnergyRow(-1), 2.31, 0.01);
    t.checkInexact(this.grid.rootPixel.getMaximumEnergyRow(-1), 3.41, 0.01);

    t.checkInexact(new BorderPixel().getMaximumEnergyRow(70), 70.0, 0.01);
    t.checkInexact(new BorderPixel().getMaximumEnergyRow(68), 68.0, 0.01);
  }

  void testIsWellFormed(Tester t) {
    this.initConds();

    t.checkExpect(this.grid2.rootPixel.isWellFormedCol(), true);
    t.checkExpect(this.grid2.rootPixel.isWellFormedRow(), true);

    t.checkExpect(this.grid.rootPixel.isWellFormedCol(), true);
    t.checkExpect(this.grid.rootPixel.isWellFormedRow(), true);

    // remove a seam
    this.grid2.removeSeamHorizontal(false);
    this.grid2.removeSeamVertical(false);

    this.grid.removeSeamHorizontal(false);
    this.grid.removeSeamVertical(false);

    t.checkExpect(this.grid2.rootPixel.isWellFormedCol(), true);
    t.checkExpect(this.grid2.rootPixel.isWellFormedRow(), true);

    t.checkExpect(this.grid.rootPixel.isWellFormedCol(), true);
    t.checkExpect(this.grid.rootPixel.isWellFormedRow(), true);

    t.checkExpect(((Pixel) this.grid.rootPixel.getRight()).isWellFormedCol(), true);
    t.checkExpect(((Pixel) this.grid.rootPixel.getRight()).isWellFormedRow(), true);

    // make a bad pixel
    APixel rightPixel = this.grid.rootPixel.getRight();
    rightPixel.changeNeighbor(new BorderPixel(), 3);
    rightPixel.changeNeighbor(new Pixel(Color.blue), 1);

    t.checkExpect(((Pixel) this.grid.rootPixel.getRight()).isWellFormedCol(), true);
    t.checkExpect(((Pixel) this.grid.rootPixel.getRight()).isWellFormedRow(), false);

    this.initConds();
    // make a bad pixel
    APixel bottomPixel = this.grid.rootPixel.getBottom();
    bottomPixel.changeNeighbor(new BorderPixel(), 0);
    bottomPixel.changeNeighbor(new Pixel(Color.blue), 2);

    t.checkExpect(((Pixel) this.grid.rootPixel.getBottom()).isWellFormedCol(), false);
    t.checkExpect(((Pixel) this.grid.rootPixel.getBottom()).isWellFormedRow(), true);

  }

}