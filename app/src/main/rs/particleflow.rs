#pragma version(1)
#pragma rs java_package_name(com.nfaralli.particleflow)

// Colors of slow and fast particles. Particles colors are interpolated based on these two colors
// and hueDirection.
// TODO: put HSV in a float4
float slowHue;
float slowSaturation;
float slowValue;
float fastHue;
float fastSaturation;
float fastValue;
int hueDirection;  // 0 for clockwise, 1 for counterclockwise

// Force coefficients.
float f01AttractionCoef;
float f01DragCoef;

// Screen resolution. Should be set before calling initParticles.
float width = 100.0f;
float height = 100.0f;

// Initial touch positions (attraction points).
// Negative values are used to disable an attraction point.
float2 *gTouch;

// Arrays containing the coordinates, velocity, and color of the particles.
// MUST have the same size.
float2 *position;
float2 *delta;
float4 *color;

/**
 * Transforms HSV components into RGBA components.
 * S and V must be within [0, 1] and H must be within [0, 1) (e.g. H=0.5 -> 180 degrees = Cyan).
 * no check performed on H, S, and V.
 * R, G, and B are within [0, 1] and A=1
 */
static float4 hsv2rgba(float h, float s, float v) {
    float4 rgba;
    float h6 = 6 * h;
    float r, g, b;  // NOT the actual rgb values.
    float coef;
  
    if (h6 < 1) {
        r = 0;
        g = 1 - h6;
        b = 1;
    } else if (h6 < 2) {
        r = h6 - 1;
        g = 0;
        b = 1;
    } else if (h6 < 3) {
        r = 1;
        g = 0;
        b = 3 - h6; 
    } else if (h6 < 4) {
        r = 1;
        g = h6 - 3;
        b = 0;
    } else if (h6 < 5) {
        r = 5 - h6;
        g = 1;
        b = 0;
    } else {
        r = 0;
        g = 1;
        b = h6 - 5;
    }
  
    coef = v * s;
    rgba.r = v - coef * r;
    rgba.g = v - coef * g;
    rgba.b = v - coef * b;
    rgba.a = 1.0f;
    return rgba;
}

/**
 * Get the hue based on coef, which must be within [0, 1].
 * The returned value will be within [0, 1) (which corresponds to the usual range [0, 360)).
 */
static float getHue(float coef) {
    float hue;
    float sh = slowHue;
    float fh = fastHue;
    if (sh < fh && hueDirection == 0) {
        sh += 1;
    } else if (sh > fh && hueDirection == 1) {
        fh += 1;
    }
    hue = (1 - coef) * sh + coef * fh;
    if (hue >= 1) {
        hue -= 1;
    }
    return hue;
}

/**
 * Get the saturation based on coef, which must be within [0, 1].
 * The returned value will be within [0, 1].
 */
static float getSaturation(float coef) {
    return (1 - coef) * slowSaturation + coef * fastSaturation;
}


/**
 * Get the value based on coef, which must be within [0, 1].
 * The returned value will be within [0, 1].
 */
static float getValue(float coef) {
    return (1 - coef) * slowValue + coef * fastValue;
}

/**
 * Returns a coefficient in the range [0, 1] based on the speed v.
 * 0 corresponds to low speeds and 1 corresponds to high speeds.
 */
static float getSpeedCoef(float2 v) {
    float coef;
    coef = log(v.x * v.x + v.y * v.y + 1) / 4.5f;  // Use + 1 to have positive log values.
    if(coef > 1.0f) {
        coef = 1.0f;
    }
    return coef;
}

/**
 * Initialize the particles.
 * Uniform distribution over a disk of diameter the diameter of the screen.
 */
void initParticles()
{   
    int size = rsAllocationGetDimX(rsGetAllocation(position));
    float2 *pt = position;
    float2 *d = delta;
    float4 *c = color;
    float radius = sqrt(width*width + height*height) / 2;
    float r, theta;
    float speedCoef;
    for (int i = 0; i < size; i++, pt++, d++, c++) {
    	r = radius * sqrt(rsRand(1.f));
    	theta = rsRand(6.28318530718f);
    	pt->x = (width/2) + r*cos(theta);
    	pt->y = (height/2) + r*sin(theta);
    	d->x = 0;
    	d->y = 0;
    	speedCoef = getSpeedCoef(*d);
    	*c = hsv2rgba(getHue(speedCoef), getSaturation(speedCoef), getValue(speedCoef));
    }
}

/**
 * Update the particles.
 * Compute the force due to each attraction points and get the corresponding acceleration, velocity
 * and new position of particle #index.
 */
void __attribute__((kernel)) updateParticles(int index) {
    int numTouch = rsAllocationGetDimX(rsGetAllocation(gTouch));
    float speedCoef, theta;
    float diffSqNorm;
    float2 diff, acc;
    float2 *pt = position + index;
    float2 *d = delta + index;
    float4 *c = color + index;
    acc.x = acc.y = 0;
    for(int i=0; i<numTouch; i++){
        if (gTouch[i].x >=0) {
            diff = gTouch[i] - *pt;
            diffSqNorm = diff.x * diff.x + diff.y * diff.y;
            if (diffSqNorm < 0.1f) {
                theta = rsRand(6.28318530718f);
                diff.x = cos(theta);
                diff.y = sin(theta);
                diffSqNorm = 1;
            }
            acc += (f01AttractionCoef / diffSqNorm) * diff;
        }
    }
    *d += acc;
    *pt += *d;
    speedCoef = getSpeedCoef(*d);
    *c = hsv2rgba(getHue(speedCoef), getSaturation(speedCoef), getValue(speedCoef));
    *d *= f01DragCoef;
}
