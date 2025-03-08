mkdir -p fastlane\metadata\android\en-US
mkdir -p fastlane\metadata\android\en-US\images\phoneScreenshots

# Example command file for special build instructions
# Uncomment and modify as needed
# cd app
# ./gradlew assembleRelease 

# Create basic metadata files
echo Cast App > fastlane\metadata\android\en-US\title.txt
echo Stream content from your device to Chromecast, Smart TVs and other devices > fastlane\metadata\android\en-US\short_description.txt

# For full description, create it manually or use PowerShell
# PowerShell version:
@"
Cast App allows you to easily stream content from your Android device to Chromecast, Smart TVs, and other casting-enabled devices.

Features:
- Stream local media files (videos, photos, music)
- Cast web content
- Support for multiple casting protocols
- Simple, intuitive interface
- No ads or tracking
- Completely open source
"@ | Out-File -FilePath fastlane\metadata\android\en-US\full_description.txt -Encoding utf8

# Create changelog
echo Initial release > fastlane\metadata\android\en-US\changelogs\1.txt

# Copy your screenshots to the screenshots directory
# Example (replace with actual paths to your screenshots):
# copy path\to\screenshot1.png fastlane\metadata\android\en-US\images\phoneScreenshots\1.png
# copy path\to\screenshot2.png fastlane\metadata\android\en-US\images\phoneScreenshots\2.png

# Make sure your app builds correctly
cd app
.\gradlew.bat assembleRelease
cd ..

# Clone the F-Droid data repository
git clone https://gitlab.com/fdroid/fdroiddata.git
cd fdroiddata

# Create a new branch for your app
git checkout -b add-castapp

# Copy your metadata file to the repository
copy ..\metadata\com.samyak.castapp.yml metadata\

# Commit your changes
git add metadata\com.samyak.castapp.yml
git commit -m "Add Cast App"

# Push to your fork (you'll need to create a GitLab account and fork the repository first)
git remote add myfork https://gitlab.com/YOUR_USERNAME/fdroiddata.git
git push -u myfork add-castapp

# Install Docker Desktop for Windows from https://www.docker.com/products/docker-desktop

# Pull the F-Droid server Docker image
docker pull registry.gitlab.com/fdroid/fdroidserver:latest

# Run the F-Droid build check in Docker
docker run --rm -v "%CD%:/app" -w /app registry.gitlab.com/fdroid/fdroidserver:latest fdroid build --verbose -f com.samyak.castapp 