package com.capitolssg.forensics;

import com.capitolssg.forensics.cm.CopyMove;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by stephan on 10/2/16.
 */
@RestController
public class CopyMoveController {

    @RequestMapping(path="/copymove")
    public CopyMove copyMove(
            @RequestParam(value="image") String image,
            @RequestParam(value="maxDifference", required = false, defaultValue = "64") float maxDifference,
            @RequestParam(value="minShift", required = false, defaultValue = "10") float minShift,
            @RequestParam(value="minStdDev", required = false, defaultValue = "10") float minStdDev,
            @RequestParam(value="quantizationLevels", required = false, defaultValue = "128") int quantizationLevels,
            @RequestParam(value="heatRadius", required = false, defaultValue = "10") int heatRadius,
            @RequestParam(value="suspectedCopies", required = false, defaultValue = "20") int suspectedCopies

    ) {
        byte[] bytes = Base64Utils.decodeFromString(image);
        return new CopyMove(bytes, maxDifference, minShift, minStdDev, quantizationLevels, heatRadius, suspectedCopies);
    }
}
