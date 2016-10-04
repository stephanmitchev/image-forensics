package com.capitolssg.forensics;

import com.capitolssg.forensics.ela.ELA;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by stephan on 10/2/16.
 */
@RestController
public class ELAController {

    @RequestMapping(path="/ela")
    public ELA ela(
            @RequestParam(value="image") String image,
            @RequestParam(value="ampFactor", required = false, defaultValue = "5") float ampFactor,
            @RequestParam(value="quantizationLevels", required = false, defaultValue = "128") int quantizationLevels

            ) {
        byte[] bytes = Base64Utils.decodeFromString(image);
        return new ELA(bytes, ampFactor, quantizationLevels);
    }
}
