#!/usr/bin/perl -w

MAIN:
{
	my $cmdline_args_length = @ARGV;
	if($cmdline_args_length < 1)
	{
		print("Usage : parseProperties.pl <filename1>\n or\nUsage : parseProperties.pl <filename1> <filename2>\n");
		exit 1;
	}
	if($cmdline_args_length == 1)
	{
		$pro_file =`basename $ARGV[0]`;
		open(INPUTFILE1,$ARGV[0]) || die "Cannot open the file: $!";
		%firstfile_properties = &process_files("INPUTFILE1");
		close(INPUTFILE1) || die "Cannot close the file : $!";
#		&print_header();
		$total_dup_keys = &find_total_duplicate_entries("0");
		$total_dup_values = &find_total_duplicate_entries("1");
		&find_duplicate_keys();
		&find_duplicate_values();
		#&print_footer();
	}
	elsif($cmdline_args_length == 4)
	{
		$pro_file =`basename $ARGV[0]`;
		$pro1_file =`basename $ARGV[1]`;
		$olden_file =`basename $ARGV[2]`;
                $newen_file =`basename $ARGV[3]`;
		open(INPUTFILE1,$ARGV[0]) || die "Cannot open the file: $!";
		$pro_lan_proper= substr $pro1_file,index($pro1_file, '_');
		$pro_lan_proper1=substr $pro_lan_proper ,1;
		$len=length($pro_lan_proper1);
		$val= $len - 11;
		$lan_value=substr $pro_lan_proper1,0,$val-1;
		open(INPUTFILE2,$ARGV[1]) || die "Cannot open the file: $!";	
		%firstfile_properties = &process_files("INPUTFILE2");
#		$total_dup_keys = &find_total_duplicate_entries("0");
#		$total_dup_values = &find_total_duplicate_entries("1");
#		&print_header(); 
		$total_dup_keys = &find_total_duplicate_entries("0");
	  $total_dup_values = &find_total_duplicate_entries("1");
		$second_pro_file=`basename $ARGV[1]`;
		open(INPUTFILE1,$ARGV[0]) || die "Cannot open the file  $!";
			%firstfile_properties = &process_files("INPUTFILE1");
			%firstfiletemphash = %subproperty_hash;
		close(INPUTFILE1) || die "Cannot close file : $!";
		open(INPUTFILE2,$ARGV[1]) || die "Cannot open the file: $!";
			%secondfile_properties = &process_files("INPUTFILE2");
			%secondfiletemphash = %subproperty_hash;
		close(INPUTFILE2) || die "Cannot close file : $!";
		open(INPUTFILE3,$ARGV[2]) || die "Cannot open the file: $!";
                        &process_files("INPUTFILE3");
                        %oldfile_keyvalue_hash = %keyvalue_hash;
                close(INPUTFILE3) || die "Cannot close file : $!";
                open(INPUTFILE4,$ARGV[3]) || die "Cannot open the file: $!";
                         &process_files("INPUTFILE4");
                        %newfile_keyvalue_hash = %keyvalue_hash;
                close(INPUTFILE4) || die "Cannot close file : $!";
#		&find_duplicate_keys();
#		&find_duplicate_values();
		if($olden_file eq $newen_file)
                {
			&get_modified_keys_cnt();
                }
		&find_added_deleted_keys();
		&find_suspected_keys();
		
	}
	elsif($cmdline_args_length == 3)
	{
		open(INPUTFILE1,$ARGV[0]) || die "Cannot open the file  $!";
		 %firstfile_properties = &process_files("INPUTFILE1");
                 %firstfiletemphash = %subproperty_hash;
		close(INPUTFILE1) || die "Cannot close file : $!";
		open(INPUTFILE2,$ARGV[1]) || die "Cannot open the  $!";
		%secondfile_properties = &process_files("INPUTFILE2");
		%newfile_keyvalue_hash = %keyvalue_hash;
		close(INPUTFILE2) || die "Cannot close file : $!";
		&get_modified_keys_cnt();
		&get_modified_keys();
	}
	 elsif($cmdline_args_length == 2)
        {
		 open(INPUTFILE1,$ARGV[0]) || die "Cannot open the file  $!";
                        %firstfile_properties = &process_files("INPUTFILE1");
                        %firstfiletemphash = %subproperty_hash;
                close(INPUTFILE1) || die "Cannot close file : $!";
                open(INPUTFILE2,$ARGV[1]) || die "Cannot open the file: $!";
                        %secondfile_properties = &process_files("INPUTFILE2");
                        %secondfiletemphash = %subproperty_hash;
                close(INPUTFILE2) || die "Cannot close file : $!";
		&find_added_deleted_keys();
        }

}

sub process_files
{
	my($filename) = @_;
	my($linecount,$readline,$indexval,$read_property,$read_value,$new_key_string,$new_keyvalue_string)=(0,'',0,'','','','');
	%property_occurences_hash = ();
	%subproperty_hash = ();
	%value_occurences_hash = ();
	%subvalue_hash=();
	%keyvalue_hash=(); #To store the key-value pairs for Modified keys report	
	while(<$filename>)
	{
		$linecount = $linecount + 1;
		$readline = $_;
		chomp($readline);
		$readline = &trim($readline);

		if(($readline =~ /^#/) || ($readline =~ /^[\n]/) || ($readline eq ''))
		{
			next;
		}
		else
		{
			$indexval = index($readline,"=");
			
			$read_property = substr($readline,0,$indexval);
			$read_property =~ s/^[\s]*//;
			$read_property =~ s/[\s]*$//;

			$read_value = substr($readline,$indexval+1);
			$read_value =~ s/^[\s]*//;
			$read_value =~ s/[\s]*$//;
			
			$property_occurences_hash{$read_property}++;
			$new_key_string = $read_property.",".$property_occurences_hash{$read_property}.",".$linecount;
			$subproperty_hash{$new_key_string} = $readline;
			$value_occurences_hash{$read_value}++;
			$new_keyvalue_string = $read_value.",".$value_occurences_hash{$read_value}.",".$linecount;
			$subvalue_hash{$new_keyvalue_string} = $readline;
			$keyvalue_hash{$read_property} = $read_value;
		
		}
	}
	return %property_occurences_hash;
}

sub find_duplicate_keys
{
	my($propval,$subpropval,$subprop_value,$occur)=('','','',0);
	my $line_occur = '';
	my $line_num = 0;
	
#	printf ("<center><a href='#top'>GO TO TOP</a> <a name='duplicatekeys'> </a>");
	printf ("DUPLICATE KEYS ($total_dup_keys)\n");
#	printf("<table border=1>");
	foreach $propval (sort keys(%property_occurences_hash))
	{
		if($property_occurences_hash{$propval} > 1)
		{
#			printf("<tr>");
#			printf("<td>%s</td>", $propval . "($property_occurences_hash{$propval})");
#			printf("<td>");
			foreach $subpropval (sort keys(%subproperty_hash))
			{
				$occur = &find_nth_last_index($subpropval,",",2);
				$subprop_value = substr($subpropval,0,$occur);
				$line_occur = &find_nth_last_index($subpropval, ",", 1);
				$line_num = substr($subpropval,$line_occur+1);
				if($subprop_value eq $propval)
				{
								#printf("%s<br> ","Line $line_num : " . $subproperty_hash{$subpropval});
					printf("Line $line_num :  . $subproperty_hash{$subpropval}\n");
				}
			}
#			printf("</td>");
#			printf("</tr>");
		}
	}
#	printf("</table>");
}

sub find_total_duplicate_entries
{
	my ($type) = @_;
	my $count = 0;
	my %local_hash = %property_occurences_hash;
	if($type eq "1")
	{
					%local_hash = %value_occurences_hash;
	}
	foreach $propval (sort keys(%local_hash))
	{
					if($local_hash{$propval} > 1)
					{
									$count = $count + 1;
					}
	}
	return $count;
}

sub find_duplicate_values
{
        my($propval,$subpropval,$subprop_value,$occur)=('','','',0);
				my $line_occur = '';
			  my $line_num = 0;
#	printf ("<a href='#top'>GO TO TOP</a> <a name='duplicatevalues'> </a>");
	printf ("DUPLICATE VALUES ($total_dup_values)\n\n");
#	printf("<table border=1>");
	foreach $propval (sort keys(%value_occurences_hash))
	{
		if($value_occurences_hash{$propval} > 1)
		{
#			printf("<tr>");
#			printf("<td>%s</td>", $propval);
#			printf("<td>");
			foreach $subpropval (sort keys(%subvalue_hash))
			{
#				@subproparray = split(',',$subpropval);
				$occur = &find_nth_last_index($subpropval,",",2);
				$subprop_value = substr($subpropval,0,$occur);
				$line_occur = &find_nth_last_index($subpropval, ",", 1);
        $line_num = substr($subpropval,$line_occur+1);
								
				if($subprop_value eq $propval)
                                {
#					printf("%s<br>", "Line $line_num : " . $subvalue_hash{$subpropval});
					printf("Line $line_num :  . $subvalue_hash{$subpropval}\n");
#					printf("%d %s<br>",$subproparray[2],$subprop{$subpropval});
                                }
			}
#			printf("</td>");
#			printf("</tr>");
		}
	}
#	printf("</table>");
}

sub find_added_deleted_keys
{
	my ($keys) = ('');
	foreach $keys (sort keys(%firstfile_properties))
	{
		foreach $subkeys (sort keys(%secondfile_properties))
		{	
			if($keys eq $subkeys)
			{
				delete $firstfile_properties{$keys};
				delete $secondfile_properties{$keys};
				last;
			}
		}
	}
	my $misskeycnt = 0;
	my $addkeycnt = 0;
	foreach $keys (sort keys (%firstfile_properties))
	{
		$misskeycnt=$misskeycnt+1;
	}
	foreach $keys (sort keys (%secondfile_properties))
        {
        	$addkeycnt=$addkeycnt+1;
	}
	my $suspectkeycnt = 0;
	%base_non_duplicate_keys = &get_non_duplicate_keys("1");
        %locale_non_duplicate_keys = &get_non_duplicate_keys("2");
        foreach $subkeys (sort keys(%base_non_duplicate_keys))
        {
                $base_key_value = $base_non_duplicate_keys{$subkeys};
                if (exists $locale_non_duplicate_keys{$subkeys})
                {
                        $locale_key_value = $locale_non_duplicate_keys{$subkeys};
                        if($base_key_value eq $locale_key_value)
                        {
                        $suspectkeycnt=$suspectkeycnt+1;
                        }
                }
        }	
#	printf ("1.<a href ='#duplicatekeys'>DUPLICATE KEYS ($total_dup_keys) </a><br>2.<a href ='#duplicatevalues'>DUPLICATE VALUES ($total_dup_values) </a><br>3.<a href='#addedkey_lang'>ADDED KEYS in $pro_file</a><br>4.<a href='#addedkeyen'>MISSING KEYS in $pro_file</a>5. <a href='#suspected'>SUSPECTED ENTRIES in $pro_file</a>");
#	printf ("1.<a href='#addedkeyen'>MISSING KEYS in $second_pro_file ($misskeycnt)</a><br>2.<a href='#addedkey_lang'>ADDED KEYS in $second_pro_file($addkeycnt)</a><br>3.<a href='#suspected'>SUSPECTED ENTRIES in $pro_file ($suspectkeycnt)</a><br>4.<a href ='#duplicatekeys'>DUPLICATE KEYS ($total_dup_keys) </a><br>4.<a href ='#duplicatevalues'>DUPLICATE VALUES ($total_dup_values) </a>");
#	print ("<center><br><br>");
#	print ("<table border =1>");
#	print ("<a href='#top'>GO TO TOP</a><table border =1>");
# print ("<tr><a name='addedkeyen'><th>MISSING KEYS in $second_pro_file($misskeycnt)</th></tr>");
#  print ("MISSING KEYS in $second_pro_file($misskeycnt)\n");
	%base_non_duplicate_keys = &get_non_duplicate_keys("1");
	%locale_non_duplicate_keys = &get_non_duplicate_keys("2");
	foreach $keys (sort keys (%firstfile_properties))
	{
		$base_key_val = $base_non_duplicate_keys{$keys};
		print ("$keys=$base_key_val\n");
		
	}
#	print ("</table>");

#	print ("<br><br><table border =1>");
#	print ("<br><br><a href='#top'>GO TO TOP</a><table border =1>");
#  print ("ADDED KEYS in $second_pro_file($addkeycnt) \n");
#	foreach $keys (sort keys (%secondfile_properties))
#	{
#					$base_key_val = $locale_non_duplicate_keys{$keys};
#		print ("$keys=$base_key_val\n");
#	}
#	print ("</table>");
#	print ("</center>");
}

sub find_suspected_keys
{
	%base_non_duplicate_keys = &get_non_duplicate_keys("1");
	%locale_non_duplicate_keys = &get_non_duplicate_keys("2");

	my ($subkeys,$base_key_value,$locale_key_value) = ("","","");

	my $suspectkeycnt = 0;
	foreach $subkeys (sort keys(%base_non_duplicate_keys))
        {
                $base_key_value = $base_non_duplicate_keys{$subkeys};
                if (exists $locale_non_duplicate_keys{$subkeys})
                {
                        $locale_key_value = $locale_non_duplicate_keys{$subkeys};
                        if($base_key_value eq $locale_key_value)
                        {
                  	$suspectkeycnt=$suspectkeycnt+1;             
                        }
                }
        }

#	print ("<center><br><br>");
#	print ("<table border =1>");
#	print ("<a href='#top'>GO TO TOP</a><table border =1>");
	print ("SUSPECTED ENTRIES in $second_pro_file ($suspectkeycnt)\n");
				
	foreach $subkeys (sort keys(%base_non_duplicate_keys))
	{
		$base_key_value = $base_non_duplicate_keys{$subkeys};
		if (exists $locale_non_duplicate_keys{$subkeys})
		{
			$locale_key_value = $locale_non_duplicate_keys{$subkeys};
			if($base_key_value eq $locale_key_value)
			{
				print("$subkeys =  $locale_key_value\n");
			}
		}
	}
#	print ("</table>");
#	print ("</center>");
}

sub get_non_duplicate_keys
{
	%non_duplicate_hash = ();
	my($type) = @_;
	my %local_duplicate_hash = ();
	my %local_keys_hash = ();
	
	my ($readline,$commaoccur,$hash_key,$indexval) = ("",0,"",0);
	if($type eq "1")
	{
					%local_duplicate_hash = %firstfile_properties;
					%local_keys_hash = %firstfiletemphash;
	}
	else
	{
					%local_duplicate_hash = %secondfile_properties;
					%local_keys_hash = %secondfiletemphash;
	}

	foreach $seckeys (sort keys (%local_keys_hash))
	{
					$commaoccur = &find_nth_last_index($seckeys,",",2);
					$hash_key = substr($seckeys,0,$commaoccur);
									$readline = $local_keys_hash{$seckeys};
									$indexval = index($readline,"=");

									$read_value = substr($readline,$indexval+1);
									$read_value =~ s/^[\s]*//;
									$read_value =~ s/[\s]*$//;

									$non_duplicate_hash{$hash_key} = $read_value;
	}
	return %non_duplicate_hash;
}

sub get_modified_keys
{
	my %old_keyvalue_hash = %oldfile_keyvalue_hash;
	my %new_keyvalue_hash = %newfile_keyvalue_hash;
	my $value_old = "";
	my $value_new = "";
#	print ("<html><body>");
#	print ("<br><br><table border =1>");
	print ("MODIFIED KEYS in English Property File ($mod_key_cnt) \n");
	foreach $keys (sort keys (%new_keyvalue_hash))
	{
		if(exists $old_keyvalue_hash{$keys})
		{
			##Key exists in both hash. Hence compare values
			$value_old = $old_keyvalue_hash{$keys};
			$value_new = $new_keyvalue_hash{$keys};

			if($value_old ne $value_new)
			{
				## Key is modified
				print ("[before] $keys = $value_old\n [after] $keys = $value_new\n");
			}
		}
	}
#	print ("</table></body></html>");
}
sub get_modified_keys_cnt
{
        my %old_keyvalue_hash = %oldfile_keyvalue_hash;
        my %new_keyvalue_hash = %newfile_keyvalue_hash;
        my $value_old = "";
        my $value_new = "";
        $mod_key_cnt = 0;
        foreach $keys (sort keys (%new_keyvalue_hash))
        {
                if(exists $old_keyvalue_hash{$keys})
                {
                        $value_old = $old_keyvalue_hash{$keys};
                        $value_new = $new_keyvalue_hash{$keys};

                        if($value_old ne $value_new)
                        {
                                $mod_key_cnt=$mod_key_cnt+1;
                        }
                }
        }
}




sub print_header
{
	print ("<html>");
	print ("<head><title>PROPERTIES</title></head>");
	print ("<body><center><h3>I18N Diff Report Between English Property File and $lan_value Property File </h3></center>");
}

sub print_footer
{
	print ("</center></body>");
	print ("</html>");
}

sub find_nth_last_index ##Function for finding the nth last occurence of a character within a string
{
	my($string,$char,$occur_val)=@_;
	my($i,$string_length,$char_occur,$chr)=(0,0,0,"");
	local $second_occur=0;
	$string_length=length($string);
	for($i=$string_length-1; $i>=0 ;$i--)
	{
		$chr = substr($string,$i,1);
		if($chr eq ",")
		{
			$char_occur = $char_occur + 1;
			if($char_occur == $occur_val)
			{
				$second_occur = $i;
				return $second_occur;
			}
		}
	}
}

sub print_hash
{
	my(%input_hash) = @_;
	foreach $keys (keys (%input_hash))
	{
		print("$keys : $input_hash{$keys}\n");
	}
}

sub trim
{
	local($get_string) = @_;
	chomp($get_string);
	$get_string =~ s/^[\s]*//;
	$get_string =~ s/[\s]*$//;

	return $get_string;
}
