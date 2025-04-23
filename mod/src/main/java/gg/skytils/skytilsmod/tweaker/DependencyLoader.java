/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.skytils.skytilsmod.tweaker;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static gg.skytils.skytilsmod.tweaker.TweakerUtil.addToClasspath;

public class DependencyLoader {

    private static final String MAVEN_CENTRAL_ROOT = "https://repo1.maven.org/maven2/";

    public static void loadDependencies() {

    }

    public static File loadDependency(String path, boolean isMod) throws Throwable {
        File downloadLocation = new File("./libraries/" + path);
        Path downloadPath = downloadLocation.toPath();

        downloadLocation.getParentFile().mkdirs();
        if (!downloadLocation.exists() || Files.size(downloadPath) == 0) {
            try (InputStream in = new URL(MAVEN_CENTRAL_ROOT + path).openStream()) {
                Files.copy(in, downloadPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        System.out.printf("Dependency size for %s: %s%n", path.substring(path.lastIndexOf('/') + 1), Files.size(downloadPath));

        // TODO
        addToClasspath(downloadLocation.toURI().toURL());

        //#if MC==10809 && FORGE
        //$$ if (!isMod) {
        //$$     CoreModManager.getIgnoredMods().add(downloadLocation.getName());
        //$$ }
        //#endif

        return downloadLocation;
    }
}
