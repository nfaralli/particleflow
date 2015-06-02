#pragma version(1)
#pragma rs java_package_name(com.nfaralli.particleflow)

// Saturation and Value components of the particles color.
#define SAT 0.7f
#define VAL 1.0f

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
 * H, S, and V must be within [0, 1) (e.g. H=0.5 -> 180 degrees = Cyan)
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
 * Get a Hue value based on a velocity vector v.
 * Hue will range from blue (0.6667) for low velocity
 * to red (0.0) for high velocity using a logarithmic scale.
 */
static float getHue(float2 v) {
    float hue;
    hue = log(v.x * v.x + v.y * v.y + 1) / 4.5f;  // Use + 1 to have positive log values.
    if(hue > 1.0f) {
        hue = 1.0f;
    }
    hue = 2.0f / 3.0f * (1.0f - hue);
    return hue;
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
    for (int i = 0; i < size; i++, pt++, d++, c++) {
    	r = radius * sqrt(rsRand(1.f));
    	theta = rsRand(6.28318530718f);
    	pt->x = (width/2) + r*cos(theta);
    	pt->y = (height/2) + r*sin(theta);
    	d->x = 0;
    	d->y = 0;
    	*c = hsv2rgba(getHue(*d), SAT, VAL);
    }
}

/**
 * Update the particles.
 * Compute the force due to each attraction points and get the corresponding acceleration, velocity
 * and new position of particle #index.
 */
void __attribute__((kernel)) updateParticles(int index) {
    int numTouch = rsAllocationGetDimX(rsGetAllocation(gTouch));
    float2 diff, acc;
    float2 *pt = position + index;
    float2 *d = delta + index;
    float4 *c = color + index;
    acc.x = acc.y = 0;
    for(int i=0; i<numTouch; i++){
        if (gTouch[i].x >=0) {
            diff = gTouch[i] - *pt;
            acc += (100.f/(diff.x * diff.x + diff.y * diff.y)) * diff;
        }
    }
    *d += acc;
    *pt += *d;
	if(pt->x>=0 && pt->x<=width && pt->y>=0 && pt->y<=height){
        *c = hsv2rgba(getHue(*d), SAT, VAL);
    }
    *d *= 0.96f;
}
