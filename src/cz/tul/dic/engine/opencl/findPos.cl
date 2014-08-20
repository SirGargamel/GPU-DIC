constant float PRECISION = 0.0001;

kernel void findPos(
        global float * buffer,
        global float * val,        
        global int * result,        
        const int groupSize, const int groupId) {        
    const int base = groupId * groupSize;
    int global_index = get_global_id(0);
// Loop sequentially over chunks of input vector
    while (global_index < groupSize) {
        float element = buffer[base + global_index];
        if (fabs(element - val[groupId]) <= PRECISION) {
            result[groupId] = global_index;
            break;
        }
        global_index += get_global_size(0);
    }
}