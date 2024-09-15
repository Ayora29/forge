#!usr/bin/perl

use strict;
use warnings;
use utf8;
use Time::HiRes qw(usleep);
use LWP::Simple;
use LWP::Protocol::https;
use JSON;
use Imager;
use Imager::File::PNG;
use Imager::File::JPEG;
use File::Path qw(make_path);
use Unicode::Normalize;

if(!defined $ARGV[0]){
	print "Veuillez indiquer le chemin du fichier txt";
	exit;
}

my $imager = Imager->new;
my $set;
my $dir;

foreach my $path (@ARGV){
	my %names = ();
	open my $file, $path or die "Could not open $path: $!";
	while( my $line = <$file>)  {
		if($line =~ m/^Code=(.+)/){
			$set = $1;
			$dir = "../../cards/en/".$set;
			if( !-d $dir){
				eval { make_path($dir) };
			}
		}
		elsif($line =~ m/^(\d+[a-z†]*)\s[C|M|U|R|L]\s/){
			my $url = "https://api.scryfall.com/cards/".lc($set)."/".$1;
			my $content = get($url); die "Couldn't GET ".$url unless defined $content;
			usleep(1000*50);
			my $json = decode_json($content);
			my $lowRes = 0;

			my $name = $json->{name};
			if($json->{image_status} eq 'lowres'){
				warn "\nWarn : low resolution for card ".$1." : ".$name;
				$lowRes = 1;
			}
			elsif($json->{image_status} ne 'highres_scan'){
				last;
			}

			if(defined $json->{card_faces} && scalar @{$json->{card_faces}} && defined @{$json->{card_faces}}[0]->{image_uris}){
				for my $face ( @{$json->{card_faces}} ) {
					$url = $lowRes?$face->{image_uris}->{large}:$face->{image_uris}->{png};
					download_card($face->{name},$url,$dir,$lowRes)
				}
			}
			else{
				my $nb = defined($names{$name})?$names{$name}+1:1;
				$names{$name} = $nb;
				if($nb>1){
					if($nb == 2) {
						my $jpegFile = $dir."/".$name.".fullborder.jpg";
						rename($jpegFile, $dir."/".$name."1.fullborder.jpg") or die( "Error in renaming " .$jpegFile );
					}
					$name = $name.$nb;
				}
				$url = $lowRes?$json->{image_uris}->{large}:$json->{image_uris}->{png};
				download_card($name,$url,$dir,$lowRes)
			}
		}
	}
	close $file;
}

sub download_card {
	my $name = $_[0];
	my $url = $_[1];
	my $directory = $_[2];
	my $lowRes = $_[3];

	$name =~ s/(\s+\/\/\s+)|:|\?//;
	my $n = NFKD($name);
	$n =~ s/\p{NonspacingMark}//g;

   	my $pngFile = "C:/Users/kika-fanch/AppData/Local/Temp/".$n.".png";
	my $jpegFile =  $directory."/".$n.".fullborder.jpg";
	
	if( !-f  $jpegFile){
		my $rc = getstore($url, $lowRes?$jpegFile:$pngFile);
		usleep(1000*50);
		if (is_error($rc)) {
			die "getstore of <$url> failed with $rc";
		}
		if($lowRes != 1){
			$imager->read(file=>$pngFile) or die $imager->errstr;
			$imager->write( type => 'jpeg', file => $jpegFile ) or die $imager->errstr;;
			unlink($pngFile) or die "Can't delete $pngFile: $!\n";
		}
	}
}