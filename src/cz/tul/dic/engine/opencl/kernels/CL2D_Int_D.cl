inline int computeIndex(const float x, const float y, const int width) {
    return (int)((y * width) + x);
}

%INT%

/**
 * @author Petr Jecmen, Technical University of Liberec
 */
kernel void CL2D_Int_D(
    global read_only int * imageA, global read_only int * imageB, 
    global read_only int * subsets, global read_only float * subsetCenters,
    global read_only float * deformationLimits, global read_only int * deformationCounts,
    global write_only float * result,    
    const int imageWidth, const int deformationCount,
    const int subsetSize, const int subsetCount,
    const int groupCountPerFacet,
    const int subsetSubCount, const int subsetBase,
    const int deformationSubCount, const int deformationBase) 
{        
    // id checks    
    const size_t subsetId = subsetBase + get_global_id(0);
    if (subsetId >= subsetBase + subsetSubCount || subsetId >= subsetCount) {
        return;
    } 
    const size_t deformationId = deformationBase + get_global_id(1);
    if (deformationId >= deformationBase + deformationSubCount || deformationId >= deformationCount) {
        return;
    }
    float deformation[%DEF_D%];
    %DEF_C%
    // index computation
    const int subsetSize2 = (2*subsetSize + 1) * (2*subsetSize + 1);
    const int subsetCoordCount = subsetSize2 * 2;
    const int baseIndexFacet = subsetId * subsetCoordCount;         
    const int baseIndexFacetCenter = subsetId * 2;
    const int baseIndexDeformation = deformationId * 6;
    // deform subset    
    float deformedFacet[(2*-1+1)*(2*-1+1)*2];    
    int index, i2, x, y;
    float dx, dy;
    for (int i = 0; i < subsetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;        
        
        x = subsets[index];
        y = subsets[index+1];

        dx = x - subsetCenters[baseIndexFacetCenter];
        dy = y - subsetCenters[baseIndexFacetCenter + 1];
        
        deformedFacet[i2] = %DEF_X%;
        deformedFacet[i2 + 1] = %DEF_Y%;
    }
    // compute correlation using ZNCC
    float deformedI[(2*-1+1)*(2*-1+1)];
    float subsetI[(2*-1+1)*(2*-1+1)];
    float meanF = 0;
    float meanG = 0; 
    for (int i = 0; i < subsetSize2; i++) {
        i2 = i*2;
        index = baseIndexFacet + i2;
                
        // subset is just array of int coords              
        subsetI[i] = imageA[computeIndex(subsets[index], subsets[index + 1], imageWidth)];
        meanF += subsetI[i];
                               
        deformedI[i] = interpolate(deformedFacet[i2], deformedFacet[i2 + 1], imageB, imageWidth);
        meanG += deformedI[i];
    } 
    meanF /= (float) subsetSize2;
    meanG /= (float) subsetSize2;
    
    float deltaF = 0;
    float deltaG = 0;   
    for (int i = 0; i < subsetSize2; i++) {                                             
        subsetI[i] -= meanF;
        deltaF += subsetI[i] * subsetI[i];
                        
        deformedI[i] -= meanG;
        deltaG += deformedI[i] * deformedI[i];
    }   
    
    float resultVal = 0;           
    if (deltaF != 0 && deltaG != 0) {
        for (int i = 0; i < subsetSize2; i++) {            
            resultVal += subsetI[i] * deformedI[i];
        }
        resultVal /= sqrt(deltaF) * sqrt(deltaG);  
    }    
    
    //store result    
    result[subsetId * deformationCount + deformationId] = resultVal;   
}